package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;

@Service
public class ComunicacaoJudicialAudienceResolver {

    public record AudienceTarget(Usuario usuario, boolean representante) {
    }

    private final UsuarioRepository usuarioRepository;
    private final LaianeProcuracaoRepository procuracaoRepository;
    private final ClienteRepository clienteRepository;

    public ComunicacaoJudicialAudienceResolver(UsuarioRepository usuarioRepository,
                                               LaianeProcuracaoRepository procuracaoRepository,
                                               ClienteRepository clienteRepository) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository, "procuracaoRepository");
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "clienteRepository");
    }

    public List<AudienceTarget> resolver(ExpedicaoJudicial expedicao, Processo processo) {
        Objects.requireNonNull(expedicao, "expedicao");
        Map<Long, AudienceTarget> out = new LinkedHashMap<>();
        String documento = normalizarDocumento(expedicao.getDestinatarioDocumento());
        if (documento != null && documento.length() == 11) {
            usuarioRepository.findByCpf(documento).ifPresent(usuario -> addTarget(out, usuario, false));
        }
        Usuario vinculado = processo != null ? processo.getUsuario() : null;
        if (vinculado != null && vinculado.getId() != null && vinculado.isAtivoESemanticoValido()) {
            String cpfVinculado = normalizarDocumento(vinculado.getCpf());
            if (documento == null || documento.isBlank() || Objects.equals(documento, cpfVinculado)) {
                addTarget(out, vinculado, false);
            }
        }
        if (processo != null && processo.getId() != null) {
            String docHash = documento == null ? null : CriptografiaPJB.hashCpfCnpj(documento);
            procuracaoRepository.findByProcessoIdAndStatusOrderByCreatedAtAsc(processo.getId(), LaianeProcuracaoStatus.ATIVA)
                    .stream()
                    .map(p -> p.getAdvogado())
                    .filter(Objects::nonNull)
                    .filter(adv -> adv.getId() != null && adv.isAtivoESemanticoValido())
                    .filter(adv -> !Objects.equals(documento, normalizarDocumento(adv.getCpf())))
                    .filter(adv -> deveNotificarRepresentante(adv, documento, docHash))
                    .forEach(adv -> addTarget(out, adv, true));
        }
        return List.copyOf(out.values());
    }

    private boolean deveNotificarRepresentante(Usuario advogado, String documento, String docHash) {
        if (advogado == null || advogado.getId() == null) {
            return false;
        }
        if (documento == null || documento.isBlank()) {
            return true;
        }
        if (docHash == null) {
            return false;
        }
        return clienteRepository.existsByCpfHashAndAdvogado_Id(docHash, advogado.getId());
    }

    private void addTarget(Map<Long, AudienceTarget> out, Usuario usuario, boolean representante) {
        if (usuario == null || usuario.getId() == null) {
            return;
        }
        AudienceTarget anterior = out.get(usuario.getId());
        if (anterior == null) {
            out.put(usuario.getId(), new AudienceTarget(usuario, representante));
            return;
        }
        out.put(usuario.getId(), new AudienceTarget(anterior.usuario(), anterior.representante() || representante));
    }

    private static String normalizarDocumento(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }
}
