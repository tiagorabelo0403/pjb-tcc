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
import java.util.Set;
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
    private final IdentidadeInstitucionalResolver institucionalResolver;

    public PeticaoIdentidadeVisualService(PeticaoIdentidadeVisualRepository repository,
                                          ObjectStoragePort storage,
                                          CurrentUserService currentUserService,
                                          IdentidadeInstitucionalResolver institucionalResolver) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.institucionalResolver = Objects.requireNonNull(institucionalResolver, "institucionalResolver");
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

    public static final String LOGO_INSTITUCIONAL_URL_PREFIX = "/api/v1/peticionamento/identidade-visual/institucional/";

    /**
     * Resolve a identidade visual efetiva do ator com consciência do cargo: para ofício institucional
     * (magistratura, MP, defensoria, procuradoria) a base é o timbre do órgão (curado ou default
     * neutro marcado) e o perfil individual só acrescenta dados pessoais de texto — nunca substitui o
     * brasão/cores do órgão. Para profissional-individual (advogado, perito), o perfil individual é
     * a autoridade, com o rótulo de registro correto (OAB, CRM, CREA…).
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> resolvePresetParaAtor(Usuario usuario) {
        if (usuario == null) {
            return Optional.empty();
        }
        IdentidadeInstitucionalResolver.IdentidadeInstitucionalDescriptor d =
                institucionalResolver.resolver(usuario.getTipoUsuario(), usuario.getUf(), usuario.getComarca());
        LinkedHashMap<String, Object> preset = new LinkedHashMap<>();

        if (d.institucional()) {
            PeticaoIdentidadeVisual curado = repository.findByEscopoAndEscopoRef("INSTITUCIONAL", d.escopoRef())
                    .filter(PeticaoIdentidadeVisual::isAtivo)
                    .orElse(null);
            if (curado != null) {
                preset.putAll(toPresetMapInstitucional(curado, d.escopoRef()));
                preset.put("brasaoCoresOrigem", "CURADORIA_ORGAO");
            } else {
                preset.putAll(d.paletaDefault());
                preset.put("brasaoCoresOrigem", IdentidadeInstitucionalResolver.ORIGEM_DEFAULT);
            }
            preset.put("classeIdentidade", d.classe().name());
            preset.put("poderRamo", d.poderRamo());
            putIfPresent(preset, "esfera", d.esfera());
            putIfPresent(preset, "nomeOrgao", d.nomeOrgao());
            putIfPresent(preset, "escopoRef", d.escopoRef());
            if (!d.cabecalhoSugerido().isEmpty()) {
                preset.put("cabecalhoSugerido", d.cabecalhoSugerido());
            }
            if (usuario.getId() != null) {
                repository.findByUsuarioId(usuario.getId())
                        .filter(PeticaoIdentidadeVisual::isAtivo)
                        .ifPresent(ind -> overlayPessoal(preset, ind));
            }
        } else {
            preset.putAll(d.paletaDefault());
            preset.put("classeIdentidade", d.classe().name());
            preset.put("poderRamo", d.poderRamo());
            putIfPresent(preset, "registroLabel", d.registroLabel());
            preset.put("brasaoCoresOrigem", IdentidadeInstitucionalResolver.ORIGEM_DEFAULT);
            if (usuario.getId() != null) {
                repository.findByUsuarioId(usuario.getId())
                        .filter(PeticaoIdentidadeVisual::isAtivo)
                        .ifPresent(ind -> preset.putAll(toPresetMap(ind)));
            }
        }
        return preset.isEmpty() ? Optional.empty() : Optional.of(Map.copyOf(preset));
    }

    private static final java.util.regex.Pattern ESCOPO_REF_FORMATO = java.util.regex.Pattern.compile("^[A-Z0-9-]{1,80}$");
    private static final Set<String> ESCOPO_REF_PREFIXOS = Set.of("PJ-", "MP-", "DP-", "PROC-");

    /**
     * Blindagem do escopoRef vindo da URL antes de virar chave de object storage ou de banco:
     * só letras maiúsculas, dígitos e hífen, e obrigatoriamente de uma família institucional conhecida
     * (PJ-, MP-, DP-, PROC-). Fecha travessia de caminho ('../') na chave de storage e rejeita chaves
     * arbitrárias — não é validação cosmética, é o gate que impede escrever fora do prefixo do órgão.
     */
    private static String requireEscopoRefValido(String escopoRef) {
        String ref = escopoRef == null ? "" : escopoRef.trim();
        if (!ESCOPO_REF_FORMATO.matcher(ref).matches()) {
            throw new IllegalArgumentException("escopoRef institucional inválido: use apenas A-Z, 0-9 e hífen.");
        }
        boolean familiaConhecida = ESCOPO_REF_PREFIXOS.stream().anyMatch(ref::startsWith);
        if (!familiaConhecida) {
            throw new IllegalArgumentException("escopoRef institucional fora das famílias reconhecidas (PJ-, MP-, DP-, PROC-).");
        }
        return ref;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obterInstitucional(String escopoRefBruto) {
        String escopoRef = requireEscopoRefValido(escopoRefBruto);
        requireAdminInstitucional();
        return repository.findByEscopoAndEscopoRef("INSTITUCIONAL", escopoRef)
                .map(e -> toPresetMapInstitucional(e, escopoRef))
                .orElseGet(() -> Map.of("escopoRef", escopoRef, "curado", false, "temLogo", false));
    }

    @Transactional
    public Map<String, Object> salvarInstitucional(String escopoRefBruto, IdentidadeVisualRequest request) {
        String escopoRef = requireEscopoRefValido(escopoRefBruto);
        requireAdminInstitucional();
        PeticaoIdentidadeVisual e = repository.findByEscopoAndEscopoRef("INSTITUCIONAL", escopoRef)
                .orElseGet(() -> PeticaoIdentidadeVisual.institucional(escopoRef));
        e.setNomeExibicao(trimToNull(request.nomeExibicao()));
        e.setNomeInstituicao(trimToNull(request.nomeInstituicao()));
        e.setCabecalhoLivre(trimToNull(request.cabecalhoLivre()));
        e.setRodapeLivre(trimToNull(request.rodapeLivre()));
        e.setPaletaPrimaria(trimToNull(request.paletaPrimaria()));
        e.setPaletaSecundaria(trimToNull(request.paletaSecundaria()));
        if (request.exibirRegistroProfissional() != null) {
            e.setExibirRegistroProfissional(request.exibirRegistroProfissional());
        }
        if (request.exibirBrasaoLogomarca() != null) {
            e.setExibirBrasaoLogomarca(request.exibirBrasaoLogomarca());
        }
        return toPresetMapInstitucional(repository.save(e), escopoRef);
    }

    @Transactional
    public Map<String, Object> uploadLogoInstitucional(String escopoRefBruto, byte[] bytes, String contentType) throws IOException {
        String escopoRef = requireEscopoRefValido(escopoRefBruto);
        requireAdminInstitucional();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("logo_empty");
        }
        if (bytes.length > LOGO_MAX_BYTES) {
            throw new IllegalArgumentException("logo_too_large");
        }
        String ct = normalizeImageContentType(bytes, contentType);
        String ext = ct.equals("image/png") ? "png" : "jpg";
        String key = "peticao-identidade/institucional/" + escopoRef + "/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + "." + ext;
        ObjectWriteResult wr = storage.put(key, new ByteArrayInputStream(bytes), bytes.length, ct,
                Map.of("escopoRef", escopoRef, "kind", "PETICAO_LOGO_INSTITUCIONAL"));
        PeticaoIdentidadeVisual e = repository.findByEscopoAndEscopoRef("INSTITUCIONAL", escopoRef)
                .orElseGet(() -> PeticaoIdentidadeVisual.institucional(escopoRef));
        e.aplicarLogo(wr.key(), ct, wr.sizeBytes(), wr.sha256());
        return toPresetMapInstitucional(repository.save(e), escopoRef);
    }

    @Transactional(readOnly = true)
    public LogoLeitura lerLogoInstitucional(String escopoRefBruto) throws IOException {
        String escopoRef = requireEscopoRefValido(escopoRefBruto);
        PeticaoIdentidadeVisual e = repository.findByEscopoAndEscopoRef("INSTITUCIONAL", escopoRef)
                .filter(PeticaoIdentidadeVisual::temLogo)
                .orElse(null);
        if (e == null) {
            return null;
        }
        ObjectReadResult read = storage.get(e.getLogoStorageKey());
        return new LogoLeitura(read, e.getLogoContentType(), e.getLogoSizeBytes(), e.getLogoSha256());
    }

    private void overlayPessoal(LinkedHashMap<String, Object> preset, PeticaoIdentidadeVisual individual) {
        putIfPresent(preset, "nomeExibicao", individual.getNomeExibicao());
        putIfPresent(preset, "cabecalhoLivre", individual.getCabecalhoLivre());
        putIfPresent(preset, "rodapeLivre", individual.getRodapeLivre());
    }

    private static Map<String, Object> toPresetMapInstitucional(PeticaoIdentidadeVisual entity, String escopoRef) {
        LinkedHashMap<String, Object> preset = new LinkedHashMap<>();
        putIfPresent(preset, "nomeExibicao", entity.getNomeExibicao());
        putIfPresent(preset, "nomeInstituicao", entity.getNomeInstituicao());
        boolean temLogo = entity.temLogo();
        if (temLogo) {
            preset.put("brasaoOuLogomarcaUri", LOGO_INSTITUCIONAL_URL_PREFIX + escopoRef + "/logo");
        }
        putIfPresent(preset, "cabecalhoLivre", entity.getCabecalhoLivre());
        putIfPresent(preset, "rodapeLivre", entity.getRodapeLivre());
        putIfPresent(preset, "paletaPrimaria", entity.getPaletaPrimaria());
        putIfPresent(preset, "paletaSecundaria", entity.getPaletaSecundaria());
        preset.put("exibirRegistroProfissional", entity.isExibirRegistroProfissional());
        preset.put("exibirBrasaoOuLogomarca", entity.isExibirBrasaoLogomarca());
        preset.put("escopoRef", escopoRef);
        preset.put("curado", true);
        preset.put("temLogo", temLogo);
        return Map.copyOf(preset);
    }

    private Usuario requireAdminInstitucional() {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null || !tipo.isAdmin()) {
            throw new AccessDeniedPjbException("A curadoria de identidade visual institucional é restrita a administrador.");
        }
        return usuario;
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
