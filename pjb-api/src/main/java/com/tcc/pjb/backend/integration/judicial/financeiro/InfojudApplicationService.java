package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaAuditSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaRequest;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaView;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialConsistencyView;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialHealthResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialOwnerView;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialRetrySummary;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialStatusResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialTimelineEntry;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialTimelineResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialWindowView;
import com.tcc.pjb.backend.model.entity.financeiro.InfojudConsulta;
import com.tcc.pjb.backend.model.repository.InfojudConsultaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InfojudApplicationService {

    private final InfojudConsultaService consultaService;
    private final InfojudConsultaRepository consultaRepository;
    private final AuditLedgerService auditLedgerService;

    public InfojudApplicationService(InfojudConsultaService consultaService,
                                     InfojudConsultaRepository consultaRepository,
                                     AuditLedgerService auditLedgerService) {
        this.consultaService = Objects.requireNonNull(consultaService);
        this.consultaRepository = Objects.requireNonNull(consultaRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "infojud.application.consulta.persist", maxMillis = 2500, critical = true)
    public InfojudConsultaResult consultar(Long processoId,
                                           String cpfCnpjConsultado,
                                           String authzTrailId,
                                           boolean delegatedOperation) {
        InfojudConsultaResult result = consultaService.consultar(new InfojudConsultaRequest(processoId, cpfCnpjConsultado, delegatedOperation), authzTrailId);
        auditLedgerService.appendSafely("INFOJUD_CONSULTA_MANUAL", "PROCESSO", String.valueOf(processoId), null, "success=" + result.success());
        return result;
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.snapshot.read", maxMillis = 1200, critical = false)
    public InfojudConsultaSnapshot snapshot(Long consultaId) {
        InfojudConsulta entity = require(consultaId);
        return new InfojudConsultaSnapshot(entity.getId(), entity.getCpfCnpjConsultado(), entity.getStatus(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.view.read", maxMillis = 1200, critical = false)
    public InfojudConsultaView view(Long consultaId) {
        InfojudConsulta entity = require(consultaId);
        return new InfojudConsultaView(entity.getId(), entity.getCpfCnpjConsultado(), entity.getStatus(), entity.getProtocoloReceita());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.audit.read", maxMillis = 1200, critical = false)
    public InfojudConsultaAuditSnapshot audit(Long consultaId) {
        InfojudConsulta entity = require(consultaId);
        return new InfojudConsultaAuditSnapshot(entity.getId(), entity.getProcessoId(), entity.getCpfCnpjConsultado(), entity.getStatus(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.status.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialStatusResult status(Long consultaId) {
        InfojudConsulta entity = require(consultaId);
        return new IntegracaoJudicialStatusResult(true, entity.getStatus(), 1L);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.health.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialHealthResult health(Long consultaId) {
        InfojudConsulta entity = require(consultaId);
        boolean healthy = !"FAILED".equalsIgnoreCase(entity.getStatus());
        return new IntegracaoJudicialHealthResult("INFOJUD", entity.getStatus(), healthy, healthy ? "consulta saudavel" : "consulta com falha pendente");
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.timeline.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialTimelineResult timeline(Long consultaId) {
        InfojudConsulta entity = require(consultaId);
        auditLedgerService.appendSafely("INFOJUD_TIMELINE_QUERY", "INFOJUD", String.valueOf(consultaId), null, "status=" + entity.getStatus());
        return new IntegracaoJudicialTimelineResult("INFOJUD", consultaId, List.of(
                new IntegracaoJudicialTimelineEntry("INFOJUD", "CRIADA", entity.getCreatedAt(), "alvo=" + entity.getCpfCnpjConsultado()),
                new IntegracaoJudicialTimelineEntry("INFOJUD", entity.getStatus(), entity.getConfirmadoEm() != null ? entity.getConfirmadoEm() : entity.getCreatedAt(), Objects.toString(entity.getProtocoloReceita(), entity.getResumoRetorno()))
        ));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.owner.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialOwnerView owner(Long consultaId) {
        InfojudConsulta entity = require(consultaId);
        return new IntegracaoJudicialOwnerView("INFOJUD", entity.getStatus(), "operadorId=" + entity.getOperadorId(), Instant.now(), entity.getId());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.window.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialWindowView window(Long consultaId) {
        InfojudConsulta entity = require(consultaId);
        String detail = entity.getProximoRetryEm() == null ? "sem retry pendente" : "retry=" + entity.getProximoRetryEm();
        return new IntegracaoJudicialWindowView("INFOJUD", entity.getStatus(), detail, Instant.now(), entity.getId());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.consistency.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialConsistencyView consistency(Long consultaId) {
        InfojudConsulta entity = require(consultaId);
        boolean consistent = entity.getProcessoId() != null && entity.getStatus() != null && !entity.getStatus().isBlank();
        return new IntegracaoJudicialConsistencyView("INFOJUD", consistent, consistent ? "registro consistente" : "registro incompleto", "pjb_infojud_consulta");
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.processo.read", maxMillis = 1200, critical = false)
    public List<InfojudConsultaView> processo(Long processoId) {
        return consultaRepository.findByProcessoIdOrderByCreatedAtDesc(requirePositive(processoId, "processoId"))
                .stream()
                .map(entity -> new InfojudConsultaView(entity.getId(), entity.getCpfCnpjConsultado(), entity.getStatus(), entity.getProtocoloReceita()))
                .toList();
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "infojud.application.retry-summary.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialRetrySummary retrySummary(int limit) {
        int effectiveLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        int pending = consultaRepository.findRetryCandidates(Instant.now()).stream().limit(effectiveLimit).toList().size();
        return new IntegracaoJudicialRetrySummary(0, 0, pending);
    }

    private InfojudConsulta require(Long consultaId) {
        return consultaRepository.findById(requirePositive(consultaId, "consultaId"))
                .orElseThrow(() -> new IllegalArgumentException("Consulta INFOJUD nao encontrada: " + consultaId));
    }

    private Long requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " obrigatorio");
        }
        return value;
    }
}