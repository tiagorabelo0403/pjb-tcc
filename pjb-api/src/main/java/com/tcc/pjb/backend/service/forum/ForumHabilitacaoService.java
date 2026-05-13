package com.tcc.pjb.backend.service.forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProcuracao;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyJsonExtractor;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ForumHabilitacaoService {

    private final LaianeProcuracaoRepository procuracaoRepository;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedgerService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<LaianeProcuracao> listarPendentes(int page, int size) {
        Usuario actor = currentUserService.getRequired();
        ensureForum(actor);
        return procuracaoRepository.findByStatusOrderByCreatedAtDesc(
                LaianeProcuracaoStatus.PENDENTE_HABILITACAO,
                PageRequest.of(page, size)
        );
    }

    @Transactional
    public LaianeProcuracao deferir(Long procuracaoId, String motivo) {
        Usuario actor = currentUserService.getRequired();
        ensureForum(actor);

        LaianeProcuracao p = procuracaoRepository.findById(procuracaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Procuracao", procuracaoId));

        if (p.getStatus() != LaianeProcuracaoStatus.PENDENTE_HABILITACAO) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "status")
                    .addMetadado("motivo", "Somente habilitações pendentes podem ser deferidas")
                    .addMetadado("statusAtual", String.valueOf(p.getStatus()));
        }
        if (p.getProcessoId() == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "processoId")
                    .addMetadado("motivo", "Habilitação pendente sem processoId (dados inconsistentes)")
                    .addMetadado("procuracaoId", procuracaoId);
        }
        validateRepresentationEnvelope(p);

        p.setStatus(LaianeProcuracaoStatus.ATIVA);
        p.setAprovadoPorId(actor.getId());
        p.setAprovadoEm(LocalDateTime.now());
        p.setDecisaoMotivo(safeMotivo(motivo));
        if (p.getInicioVigencia() == null) {
            p.setInicioVigencia(java.time.LocalDate.now());
        }

        p = procuracaoRepository.save(p);

        auditLedgerService.appendSafely(
                "HABILITACAO_DEFERIDA",
                "PROCESSO",
                String.valueOf(p.getProcessoId()),
                null,
                "procuracaoId=" + p.getId() + ";advogadoId=" + (p.getAdvogado() != null ? p.getAdvogado().getId() : null)
        );

        return p;
    }

    @Transactional
    public LaianeProcuracao indeferir(Long procuracaoId, String motivo) {
        Usuario actor = currentUserService.getRequired();
        ensureForum(actor);

        LaianeProcuracao p = procuracaoRepository.findById(procuracaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Procuracao", procuracaoId));

        if (p.getStatus() != LaianeProcuracaoStatus.PENDENTE_HABILITACAO) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "status")
                    .addMetadado("motivo", "Somente habilitações pendentes podem ser indeferidas")
                    .addMetadado("statusAtual", String.valueOf(p.getStatus()));
        }

        p.setStatus(LaianeProcuracaoStatus.INDEFERIDA);
        p.setAprovadoPorId(actor.getId());
        p.setAprovadoEm(LocalDateTime.now());
        p.setDecisaoMotivo(safeMotivo(motivo));

        p = procuracaoRepository.save(p);

        auditLedgerService.appendSafely(
                "HABILITACAO_INDEFERIDA",
                "PROCESSO",
                String.valueOf(p.getProcessoId()),
                null,
                "procuracaoId=" + p.getId() + ";advogadoId=" + (p.getAdvogado() != null ? p.getAdvogado().getId() : null)
        );

        return p;
    }

    private void validateRepresentationEnvelope(LaianeProcuracao procuracao) {
        if (procuracao == null || procuracao.getPoderes() == null || procuracao.getPoderes().isBlank()) {
            return;
        }
        try {
            Map<String, Object> policyMap = RepresentacaoProcessualPolicyJsonExtractor.extract(objectMapper, procuracao.getPoderes());
            if (policyMap.isEmpty()) {
                return;
            }
            boolean regularidade = RepresentacaoProcessualPolicyJsonExtractor.regularidadeSuficiente(objectMapper, procuracao.getPoderes());
            if (!regularidade) {
                throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "representacao")
                        .addMetadado("motivo", RepresentacaoProcessualPolicyJsonExtractor.firstAlerta(objectMapper, procuracao.getPoderes(), "Representação processual inválida para deferimento."))
                        .addMetadado("procuracaoId", procuracao.getId());
            }
            boolean exigeAtaOuTermo = RepresentacaoProcessualPolicyJsonExtractor.exigeTermoOuAta(objectMapper, procuracao.getPoderes());
            boolean possuiTermo = RepresentacaoProcessualPolicyJsonExtractor.hasReference(objectMapper, procuracao.getPoderes(), "termoAudienciaReferencia");
            boolean possuiAta = RepresentacaoProcessualPolicyJsonExtractor.hasReference(objectMapper, procuracao.getPoderes(), "ataAudienciaReferencia");
            if (exigeAtaOuTermo && !possuiTermo && !possuiAta) {
                throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "representacao")
                        .addMetadado("motivo", "A representação exige termo ou ata de audiência vinculados antes do deferimento.")
                        .addMetadado("procuracaoId", procuracao.getId());
            }
        } catch (ErroDeValidacaoException ex) {
            throw ex;
        } catch (Exception ignored) {
        }
    }



    private void ensureForum(Usuario actor) {
        TipoUsuario t = actor.getTipoUsuario();
        boolean ok = t != null && (t.isServidorJudiciario() || t.isMagistratura() || t.isAdmin());
        if (!ok) {
            throw new com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException(
                    "Apenas servidores do fórum/magistratura/admin podem decidir habilitações");
        }
    }

    private static String safeMotivo(String motivo) {
        if (motivo == null) return null;
        String m = motivo.trim();
        if (m.isBlank()) return null;
        if (m.length() > 5000) m = m.substring(0, 5000);
        return m;
    }
}
