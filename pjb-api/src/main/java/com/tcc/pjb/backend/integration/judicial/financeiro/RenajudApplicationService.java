package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialConsistencyView;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialHealthResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialOwnerView;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialRetrySummary;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialStatusResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialTimelineEntry;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialTimelineResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialWindowView;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoAuditSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoRequest;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoView;
import com.tcc.pjb.backend.model.entity.financeiro.RenajudRestricao;
import com.tcc.pjb.backend.model.repository.RenajudRestricaoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenajudApplicationService {

    private final RenajudRestricaoService restricaoService;
    private final RenajudRestricaoRepository restricaoRepository;
    private final AuditLedgerService auditLedgerService;

    public RenajudApplicationService(RenajudRestricaoService restricaoService,
                                     RenajudRestricaoRepository restricaoRepository,
                                     AuditLedgerService auditLedgerService) {
        this.restricaoService = Objects.requireNonNull(restricaoService);
        this.restricaoRepository = Objects.requireNonNull(restricaoRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "renajud.application.restricao.persist", maxMillis = 2500, critical = true)
    public RenajudRestricaoResult restringir(Long processoId,
                                             String placa,
                                             String renavam,
                                             String tipo,
                                             String authzTrailId) {
        RenajudRestricaoResult result = restricaoService.solicitarRestricao(new RenajudRestricaoRequest(processoId, tipo, placa, renavam), authzTrailId);
        auditLedgerService.appendSafely("RENAJUD_RESTRICAO_MANUAL", "PROCESSO", String.valueOf(processoId), null, "success=" + result.success());
        return result;
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.snapshot.read", maxMillis = 1200, critical = false)
    public RenajudRestricaoSnapshot snapshot(Long restricaoId) {
        RenajudRestricao entity = require(restricaoId);
        return new RenajudRestricaoSnapshot(entity.getId(), entity.getPlaca(), entity.getStatus(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.view.read", maxMillis = 1200, critical = false)
    public RenajudRestricaoView view(Long restricaoId) {
        RenajudRestricao entity = require(restricaoId);
        return new RenajudRestricaoView(entity.getId(), entity.getPlaca(), entity.getStatus(), entity.getProtocoloDenatran());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.audit.read", maxMillis = 1200, critical = false)
    public RenajudRestricaoAuditSnapshot audit(Long restricaoId) {
        RenajudRestricao entity = require(restricaoId);
        return new RenajudRestricaoAuditSnapshot(entity.getId(), entity.getProcessoId(), entity.getPlaca(), entity.getStatus(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.status.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialStatusResult status(Long restricaoId) {
        RenajudRestricao entity = require(restricaoId);
        return new IntegracaoJudicialStatusResult(true, entity.getStatus(), 1L);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.health.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialHealthResult health(Long restricaoId) {
        RenajudRestricao entity = require(restricaoId);
        boolean healthy = !"FAILED".equalsIgnoreCase(entity.getStatus());
        return new IntegracaoJudicialHealthResult("RENAJUD", entity.getStatus(), healthy, healthy ? "restricao saudavel" : "restricao com falha pendente");
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.timeline.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialTimelineResult timeline(Long restricaoId) {
        RenajudRestricao entity = require(restricaoId);
        auditLedgerService.appendSafely("RENAJUD_TIMELINE_QUERY", "RENAJUD", String.valueOf(restricaoId), null, "status=" + entity.getStatus());
        return new IntegracaoJudicialTimelineResult("RENAJUD", restricaoId, List.of(
                new IntegracaoJudicialTimelineEntry("RENAJUD", "CRIADA", entity.getCreatedAt(), "tipo=" + entity.getTipo()),
                new IntegracaoJudicialTimelineEntry("RENAJUD", entity.getStatus(), entity.getConfirmadoEm() != null ? entity.getConfirmadoEm() : entity.getCreatedAt(), Objects.toString(entity.getProtocoloDenatran(), entity.getStatus()))
        ));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.owner.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialOwnerView owner(Long restricaoId) {
        RenajudRestricao entity = require(restricaoId);
        return new IntegracaoJudicialOwnerView("RENAJUD", entity.getStatus(), "operadorId=" + entity.getOperadorId(), Instant.now(), entity.getId());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.window.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialWindowView window(Long restricaoId) {
        RenajudRestricao entity = require(restricaoId);
        String detail = entity.getProximoRetryEm() == null ? "sem retry pendente" : "retry=" + entity.getProximoRetryEm();
        return new IntegracaoJudicialWindowView("RENAJUD", entity.getStatus(), detail, Instant.now(), entity.getId());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.consistency.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialConsistencyView consistency(Long restricaoId) {
        RenajudRestricao entity = require(restricaoId);
        boolean consistent = entity.getProcessoId() != null && entity.getStatus() != null && !entity.getStatus().isBlank();
        return new IntegracaoJudicialConsistencyView("RENAJUD", consistent, consistent ? "registro consistente" : "registro incompleto", "pjb_renajud_restricao");
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.processo.read", maxMillis = 1200, critical = false)
    public List<RenajudRestricaoView> processo(Long processoId) {
        return restricaoRepository.findByProcessoIdOrderByCreatedAtDesc(requirePositive(processoId, "processoId"))
                .stream()
                .map(entity -> new RenajudRestricaoView(entity.getId(), entity.getPlaca(), entity.getStatus(), entity.getProtocoloDenatran()))
                .toList();
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "renajud.application.retry-summary.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialRetrySummary retrySummary(int limit) {
        int effectiveLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int pending = restricaoRepository.findRetryCandidates(Instant.now()).stream().limit(effectiveLimit).toList().size();
        return new IntegracaoJudicialRetrySummary(0, pending, 0);
    }

    private RenajudRestricao require(Long restricaoId) {
        return restricaoRepository.findById(requirePositive(restricaoId, "restricaoId"))
                .orElseThrow(() -> new IllegalArgumentException("Restricao RENAJUD nao encontrada: " + restricaoId));
    }

    private Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " obrigatorio");
        }
        return value;
    }
}