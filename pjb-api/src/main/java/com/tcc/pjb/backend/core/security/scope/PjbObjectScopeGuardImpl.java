package com.tcc.pjb.backend.core.security.scope;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.BoletimOcorrenciaDigital;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.BoletimOcorrenciaDigitalRepository;
import com.tcc.pjb.backend.model.repository.InqueritoPolicialDigitalRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;

/**
 * A checagem institucional desta fase é territorial (comarca/uf da lotação); distinção por
 * instituição na mesma comarca exige instituicao_id no WorkItem — fase futura.
 */
@Service
public class PjbObjectScopeGuardImpl implements PjbObjectScopeGuard {

    private final SecretariatInstitutionalVisibilityService visibilityService;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedgerService;
    private final LotacaoInstituicaoRepository lotacaoRepo;
    private final DelegaciaInstitucionalScopeService delegaciaScopeService;
    private final BoletimOcorrenciaDigitalRepository boletimOcorrenciaRepository;
    private final InqueritoPolicialDigitalRepository inqueritoRepository;

    public PjbObjectScopeGuardImpl(SecretariatInstitutionalVisibilityService visibilityService,
                                    CurrentUserService currentUserService,
                                    AuditLedgerService auditLedgerService,
                                    LotacaoInstituicaoRepository lotacaoRepo,
                                    DelegaciaInstitucionalScopeService delegaciaScopeService,
                                    BoletimOcorrenciaDigitalRepository boletimOcorrenciaRepository,
                                    InqueritoPolicialDigitalRepository inqueritoRepository) {
        this.visibilityService = Objects.requireNonNull(visibilityService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.lotacaoRepo = Objects.requireNonNull(lotacaoRepo);
        this.delegaciaScopeService = Objects.requireNonNull(delegaciaScopeService);
        this.boletimOcorrenciaRepository = Objects.requireNonNull(boletimOcorrenciaRepository);
        this.inqueritoRepository = Objects.requireNonNull(inqueritoRepository);
    }

    @Override
    public void requireAccess(TipoObjetoProtegido tipo, Long objetoId, AcaoEscopo acao) {
        switch (tipo) {
            case WORK_ITEM -> requireWorkItemAccess(objetoId, acao);
            case BOLETIM_OCORRENCIA -> requireBoletimOcorrenciaAccess(objetoId);
            case INQUERITO -> requireInqueritoAccess(objetoId);
            default -> throw new UnsupportedOperationException("Tipo de objeto ainda não suportado: " + tipo);
        }
    }

    @Override
    public boolean canAccess(TipoObjetoProtegido tipo, Long objetoId, AcaoEscopo acao) {
        try {
            requireAccess(tipo, objetoId, acao);
            return true;
        } catch (AcessoForaDeEscopoException e) {
            return false;
        }
    }

    @Override
    public Optional<EscopoNegado> explicar(TipoObjetoProtegido tipo, Long objetoId, AcaoEscopo acao) {
        try {
            requireAccess(tipo, objetoId, acao);
            return Optional.empty();
        } catch (AcessoForaDeEscopoException e) {
            return Optional.of(new EscopoNegado(e.getMotivo(), e.getMessage()));
        }
    }

    private void requireWorkItemAccess(Long id, AcaoEscopo acao) {
        WorkItem wi = requireTerritoryCheck(id);
        requireInstitutionalCheck(wi, id);
        if (acao == AcaoEscopo.CAPTURAR || acao == AcaoEscopo.MOVIMENTAR) {
            requireAssignedUserCheck(wi, id);
        }
    }

    private void requireBoletimOcorrenciaAccess(Long id) {
        BoletimOcorrenciaDigital boletim = boletimOcorrenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("BoletimOcorrenciaDigital", id));
        Usuario current = currentUserService.getRequired();
        try {
            delegaciaScopeService.requireEscopoPolicialNaDelegaciaComTerritorio(current, boletim.getUnidadeRegistro());
        } catch (IllegalStateException ex) {
            denyPoliceScope(TipoObjetoProtegido.BOLETIM_OCORRENCIA, "BOLETIM_OCORRENCIA", id);
        }
    }

    private void requireInqueritoAccess(Long id) {
        InqueritoPolicialDigital inquerito = inqueritoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("InqueritoPolicialDigital", id));
        Usuario current = currentUserService.getRequired();
        if (current.getTipoUsuario() == null) {
            denyPoliceScope(TipoObjetoProtegido.INQUERITO, "INQUERITO", id);
        }
        if (current.getTipoUsuario().isSegurancaPublica() && inquerito.getAutoridadeResponsavel() != null
                && Objects.equals(inquerito.getAutoridadeResponsavel().getId(), current.getId())) {
            return;
        }
        if (current.getTipoUsuario().isSegurancaPublica()) {
            try {
                delegaciaScopeService.requireEscopoPolicialNaUnidadeAtual(current, inquerito.getUnidadeApuracao());
                return;
            } catch (IllegalStateException ex) {
                denyPoliceScope(TipoObjetoProtegido.INQUERITO, "INQUERITO", id);
            }
        }
        if (current.getTipoUsuario().isMinisterioPublico() || current.getTipoUsuario().isMagistratura()) {
            return;
        }
        denyPoliceScope(TipoObjetoProtegido.INQUERITO, "INQUERITO", id);
    }

    private WorkItem requireTerritoryCheck(Long id) {
        try {
            return visibilityService.requireDeskAccess(id);
        } catch (ResponseStatusException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status == HttpStatus.FORBIDDEN) {
                MotivoNegacaoEscopo motivo = mapMotivoFromReason(ex.getReason());
                auditDenial("BOLA_ACCESS_DENIED", "WORK_ITEM", String.valueOf(id));
                throw new AcessoForaDeEscopoException(motivo, TipoObjetoProtegido.WORK_ITEM, id);
            }
            throw ex;
        }
    }

    private void requireInstitutionalCheck(WorkItem wi, Long id) {
        Usuario current = currentUserService.getRequired();
        List<LotacaoInstituicao> ativas = lotacaoRepo.findAtivasByUsuario(current);
        if (ativas.isEmpty()) {
            return;
        }
        List<LotacaoInstituicao> opinantes = ativas.stream()
                .filter(l -> l.getUnidade().getComarca() != null && l.getUnidade().getUf() != null)
                .toList();
        if (opinantes.isEmpty()) {
            return;
        }
        if (wi.getComarca() == null || wi.getUf() == null) {
            return;
        }
        boolean algumaOpinanteBate = opinantes.stream().anyMatch(l ->
                wi.getComarca().equalsIgnoreCase(l.getUnidade().getComarca())
                        && wi.getUf().equalsIgnoreCase(l.getUnidade().getUf()));
        if (!algumaOpinanteBate) {
            auditDenial("BOLA_ACCESS_DENIED_INSTITUTIONAL", "WORK_ITEM", String.valueOf(id));
            throw new AcessoForaDeEscopoException(
                    MotivoNegacaoEscopo.FORA_DA_COMARCA_DA_LOTACAO, TipoObjetoProtegido.WORK_ITEM, id);
        }
    }

    private void requireAssignedUserCheck(WorkItem wi, Long id) {
        if (wi.getAssignedUser() == null) {
            return;
        }
        Usuario current = currentUserService.getRequired();
        if (!wi.getAssignedUser().getId().equals(current.getId())) {
            auditDenial("BOLA_ACCESS_DENIED_OWNER", "WORK_ITEM", String.valueOf(id));
            throw new AcessoForaDeEscopoException(
                    MotivoNegacaoEscopo.WORKITEM_DE_OUTRO_USUARIO, TipoObjetoProtegido.WORK_ITEM, id);
        }
    }

    private MotivoNegacaoEscopo mapMotivoFromReason(String reason) {
        if (reason == null) return MotivoNegacaoEscopo.FORA_DA_COMARCA;
        String r = reason.toLowerCase();
        if (r.contains("sigilo")) return MotivoNegacaoEscopo.SIGILO_INCOMPATIVEL;
        if (r.contains("perfil") || r.contains("nascer")) return MotivoNegacaoEscopo.PAPEL_INSUFICIENTE;
        return MotivoNegacaoEscopo.FORA_DA_COMARCA;
    }

    private void auditDenial(String eventCode, String resourceType, String resourceId) {
        try {
            auditLedgerService.appendSafely(eventCode, resourceType, resourceId);
        } catch (Exception ignored) {
        }
    }

    private void denyPoliceScope(TipoObjetoProtegido tipo, String resourceType, Long id) {
        auditDenial("BOLA_ACCESS_DENIED_POLICE", resourceType, String.valueOf(id));
        throw new AcessoForaDeEscopoException(MotivoNegacaoEscopo.SEM_ATRIBUICAO, tipo, id);
    }
}
