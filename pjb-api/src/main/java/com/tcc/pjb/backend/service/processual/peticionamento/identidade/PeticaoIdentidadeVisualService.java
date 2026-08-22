package com.tcc.pjb.backend.service.processual.peticionamento.identidade;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.storage.ObjectReadResult;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.storage.ObjectWriteResult;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.identidade.IdentidadeVisualRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.identidade.IdentidadeVisualResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.peticionamento.PeticaoIdentidadeVisual;
import com.tcc.pjb.backend.model.repository.PeticaoIdentidadeVisualRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Perfil de identidade visual (papel timbrado) reutilizável por ator peticionante. Substitui o
 * antigo fluxo em que o logo/cabeçalho/cores vinham apenas como DTO transitório a cada sessão:
 * agora o ator salva uma vez e o perfil é aplicado em toda peça. O logo segue para object storage
 * pelo mesmo padrão de {@code UserAvatarService}, nunca como blob relacional.
 */
@Service
public class PeticaoIdentidadeVisualService {

    private static final long LOGO_MAX_BYTES = 2_000_000L;

    private final PeticaoIdentidadeVisualRepository repository;
    private final ObjectStoragePort storage;
    private final CurrentUserService currentUserService;

    public PeticaoIdentidadeVisualService(PeticaoIdentidadeVisualRepository repository,
                                          ObjectStoragePort storage,
                                          CurrentUserService currentUserService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
    }

    @Transactional(readOnly = true)
    public IdentidadeVisualResponse obterMinha() {
        Usuario usuario = requirePeticionante();
        return repository.findByUsuarioId(usuario.getId())
                .map(IdentidadeVisualResponse::from)
                .orElseGet(IdentidadeVisualResponse::vazia);
    }

    @Transactional
    public IdentidadeVisualResponse salvarMinha(IdentidadeVisualRequest request) {
        Usuario usuario = requirePeticionante();
        PeticaoIdentidadeVisual entity = repository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> new PeticaoIdentidadeVisual(usuario.getId()));
        entity.setNomeExibicao(trimToNull(request.nomeExibicao()));
        entity.setNomeInstituicao(trimToNull(request.nomeInstituicao()));
        entity.setCabecalhoLivre(trimToNull(request.cabecalhoLivre()));
        entity.setRodapeLivre(trimToNull(request.rodapeLivre()));
        entity.setPaletaPrimaria(trimToNull(request.paletaPrimaria()));
        entity.setPaletaSecundaria(trimToNull(request.paletaSecundaria()));
        if (request.exibirRegistroProfissional() != null) {
            entity.setExibirRegistroProfissional(request.exibirRegistroProfissional());
        }
        if (request.exibirBrasaoLogomarca() != null) {
            entity.setExibirBrasaoLogomarca(request.exibirBrasaoLogomarca());
        }
        return IdentidadeVisualResponse.from(repository.save(entity));
    }

    @Transactional
    public IdentidadeVisualResponse uploadLogo(byte[] bytes, String contentType) throws IOException {
        Usuario usuario = requirePeticionante();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("logo_empty");
        }
        if (bytes.length > LOGO_MAX_BYTES) {
            throw new IllegalArgumentException("logo_too_large");
        }
        String ct = normalizeImageContentType(bytes, contentType);
        String ext = ct.equals("image/png") ? "png" : "jpg";
        String key = "peticao-identidade/user/" + usuario.getId() + "/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + "." + ext;
        ObjectWriteResult wr = storage.put(
                key,
                new ByteArrayInputStream(bytes),
                bytes.length,
                ct,
                Map.of("usuarioId", String.valueOf(usuario.getId()), "kind", "PETICAO_LOGO"));
        PeticaoIdentidadeVisual entity = repository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> new PeticaoIdentidadeVisual(usuario.getId()));
        entity.aplicarLogo(wr.key(), ct, wr.sizeBytes(), wr.sha256());
        return IdentidadeVisualResponse.from(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public LogoLeitura lerLogo() throws IOException {
        Usuario usuario = requirePeticionante();
        PeticaoIdentidadeVisual entity = repository.findByUsuarioId(usuario.getId())
                .filter(PeticaoIdentidadeVisual::temLogo)
                .orElse(null);
        if (entity == null) {
            return null;
        }
        ObjectReadResult read = storage.get(entity.getLogoStorageKey());
        return new LogoLeitura(read, entity.getLogoContentType(), entity.getLogoSizeBytes(), entity.getLogoSha256());
    }

    @Transactional
    public IdentidadeVisualResponse removerLogo() {
        Usuario usuario = requirePeticionante();
        PeticaoIdentidadeVisual entity = repository.findByUsuarioId(usuario.getId()).orElse(null);
        if (entity == null) {
            return IdentidadeVisualResponse.vazia();
        }
        entity.removerLogo();
        return IdentidadeVisualResponse.from(repository.save(entity));
    }

    /**
     * Resolve o preset de identidade visual salvo do ator na mesma forma consumida pela sessão de
     * peticionamento (mesmas chaves de {@code PeticionamentoVisualIdentityRequest}). Vazio quando o
     * ator ainda não salvou perfil — nesse caso a sessão segue com o que vier no próprio request.
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> resolvePreset(Long usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }
        return repository.findByUsuarioId(usuarioId)
                .filter(PeticaoIdentidadeVisual::isAtivo)
                .map(PeticaoIdentidadeVisualService::toPresetMap);
    }

    private static Map<String, Object> toPresetMap(PeticaoIdentidadeVisual entity) {
        LinkedHashMap<String, Object> preset = new LinkedHashMap<>();
        putIfPresent(preset, "nomeExibicao", entity.getNomeExibicao());
        putIfPresent(preset, "nomeInstituicao", entity.getNomeInstituicao());
        if (entity.temLogo()) {
            preset.put("brasaoOuLogomarcaUri", IdentidadeVisualResponse.LOGO_URL);
        }
        putIfPresent(preset, "cabecalhoLivre", entity.getCabecalhoLivre());
        putIfPresent(preset, "rodapeLivre", entity.getRodapeLivre());
        putIfPresent(preset, "paletaPrimaria", entity.getPaletaPrimaria());
        putIfPresent(preset, "paletaSecundaria", entity.getPaletaSecundaria());
        preset.put("exibirRegistroProfissional", entity.isExibirRegistroProfissional());
        preset.put("exibirBrasaoOuLogomarca", entity.isExibirBrasaoLogomarca());
        preset.put("origem", "PERFIL_SALVO");
        return Map.copyOf(preset);
    }

    private Usuario requirePeticionante() {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        boolean permitido = tipo != null && (tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isProcuradoria() || tipo.isMinisterioPublico());
        if (!permitido) {
            throw new AccessDeniedPjbException("A identidade visual de peticionamento é exclusiva para advocacia, defensoria, procuradoria e Ministério Público.");
        }
        return usuario;
    }

    private static String normalizeImageContentType(byte[] bytes, String provided) {
        String p = provided != null ? provided.trim().toLowerCase() : "";
        if (p.equals("image/jpeg") || p.equals("image/jpg")) {
            return "image/jpeg";
        }
        if (p.equals("image/png")) {
            return "image/png";
        }
        if (bytes.length >= 4) {
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
                return "image/jpeg";
            }
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                return "image/png";
            }
        }
        throw new IllegalArgumentException("logo_content_type_unsupported");
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record LogoLeitura(ObjectReadResult read, String contentType, Long sizeBytes, String sha256) {
    }
}
