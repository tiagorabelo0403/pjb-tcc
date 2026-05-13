package com.tcc.pjb.backend.integration.datajud.feed;

import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointViewQuery;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalHealthQuery;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudWindowQuery;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataJudApplicationService {

    private final DataJudFeedService feedService;
    private final AuditLedgerService auditLedgerService;

    public DataJudApplicationService(DataJudFeedService feedService,
                                     AuditLedgerService auditLedgerService) {
        this.feedService = Objects.requireNonNull(feedService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "datajud.application.run.persist", maxMillis = 2500, critical = true)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedRunSummary run(String tribunalCodigo) {
        String normalized = normalize(tribunalCodigo);
        var summary = feedService.runIncremental(normalized);
        auditLedgerService.appendSafely("DATAJUD_RUN_MANUAL", "DATAJUD", normalized, null, "totalSent=" + summary.totalSent());
        return summary;
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.application.checkpoint.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointView checkpoint(String tribunalCodigo) {
        return feedService.checkpointView(normalize(tribunalCodigo));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.application.checkpoint-audit.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointAuditSnapshot checkpointAudit(String tribunalCodigo) {
        return feedService.checkpointAudit(normalize(tribunalCodigo));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.application.checkpoint-result.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointViewResult checkpointResult(String tribunalCodigo) {
        return feedService.checkpointViewResult(new DataJudCheckpointViewQuery(normalize(tribunalCodigo)));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.application.health.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedHealthSnapshot health(String tribunalCodigo) {
        return feedService.healthSnapshot(normalize(tribunalCodigo));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.application.execution-health.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedExecutionHealth executionHealth(String tribunalCodigo) {
        return feedService.executionHealth(normalize(tribunalCodigo));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.application.window.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudWindowView window(String tribunalCodigo) {
        String normalized = normalize(tribunalCodigo);
        var view = feedService.windowView(new DataJudWindowQuery(normalized));
        auditLedgerService.appendSafely("DATAJUD_WINDOW_QUERY", "DATAJUD", normalized, null, "batchSize=" + view.batchSize());
        return view;
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.application.tribunal-health.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalHealthResult tribunalHealth(String tribunalCodigo) {
        return feedService.tribunalHealth(new DataJudTribunalHealthQuery(normalize(tribunalCodigo)));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.application.execution-view.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalExecutionView executionView(String tribunalCodigo) {
        return feedService.executionView(normalize(tribunalCodigo));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "datajud.application.audit.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalAuditView audit(String tribunalCodigo) {
        String normalized = normalize(tribunalCodigo);
        var view = feedService.tribunalAuditView(normalized);
        long totalSent = view.progress() == null ? 0 : view.progress().totalSent();
        auditLedgerService.appendSafely("DATAJUD_AUDIT_QUERY", "DATAJUD", normalized, null, "totalSent=" + totalSent);
        return view;
    }

    private String normalize(String tribunalCodigo) {
        if (tribunalCodigo == null || tribunalCodigo.isBlank()) {
            throw new IllegalArgumentException("tribunalCodigo obrigatorio");
        }
        return tribunalCodigo.trim().toUpperCase(Locale.ROOT);
    }
}