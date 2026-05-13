package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.dje.domain.DjeAuditEntryView;
import com.tcc.pjb.backend.core.dje.domain.DjeBudgetView;
import com.tcc.pjb.backend.core.dje.domain.DjeConsultaPrazoCommand;
import com.tcc.pjb.backend.core.dje.domain.DjeConsultaPrazoResult;
import com.tcc.pjb.backend.core.dje.domain.DjeDispatchHealthQuery;
import com.tcc.pjb.backend.core.dje.domain.DjeDispatchHealthResult;
import com.tcc.pjb.backend.core.dje.domain.DjeDispatchWindowView;
import com.tcc.pjb.backend.core.dje.domain.DjeEdicaoHealthSnapshot;
import com.tcc.pjb.backend.core.dje.domain.DjeEdicaoMetricsView;
import com.tcc.pjb.backend.core.dje.domain.DjeFalhaQuery;
import com.tcc.pjb.backend.core.dje.domain.DjeFalhaResult;
import com.tcc.pjb.backend.core.dje.domain.DjeHealthQuery;
import com.tcc.pjb.backend.core.dje.domain.DjeHealthResult;
import com.tcc.pjb.backend.core.dje.domain.DjeLifecycleExecutionSummary;
import com.tcc.pjb.backend.core.dje.domain.DjeLifecycleHealthView;
import com.tcc.pjb.backend.core.dje.domain.DjeNotificationAuditView;
import com.tcc.pjb.backend.core.dje.domain.DjeNotificacaoQuery;
import com.tcc.pjb.backend.core.dje.domain.DjeNotificacaoResult;
import com.tcc.pjb.backend.core.dje.domain.DjeOwnerView;
import com.tcc.pjb.backend.core.dje.domain.DjeProcessoPublicationView;
import com.tcc.pjb.backend.core.dje.domain.DjePublicacaoConsultaCommand;
import com.tcc.pjb.backend.core.dje.domain.DjePublicacaoConsultaResult;
import com.tcc.pjb.backend.core.dje.domain.DjePublicationConsistencyAuditView;
import com.tcc.pjb.backend.core.dje.domain.DjePublicationConsistencyView;
import com.tcc.pjb.backend.core.dje.domain.DjePublicationMetricsView;
import com.tcc.pjb.backend.core.dje.domain.DjePublicationWindowHealthView;
import com.tcc.pjb.backend.core.dje.domain.DjePrazoHealthQuery;
import com.tcc.pjb.backend.core.dje.domain.DjePrazoHealthResult;
import com.tcc.pjb.backend.core.dje.domain.DjeSignalView;
import com.tcc.pjb.backend.core.dje.domain.DjeStatusQuery;
import com.tcc.pjb.backend.core.dje.domain.DjeStatusResult;
import com.tcc.pjb.backend.core.dje.domain.DjeTimelineQuery;
import com.tcc.pjb.backend.core.dje.domain.DjeTimelineView;
import com.tcc.pjb.backend.core.dje.domain.DjeTribunalHealthQuery;
import com.tcc.pjb.backend.core.dje.domain.DjeTribunalHealthView;
import com.tcc.pjb.backend.core.dje.domain.DjeTribunalPublicationQuery;
import com.tcc.pjb.backend.core.dje.domain.DjeTribunalPublicationResult;
import com.tcc.pjb.backend.core.dje.domain.DjeWindowView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DjeApplicationService {

    private final DjePublicacaoService djePublicacaoService;
    private final DjeProperties djeProperties;
    private final AuditLedgerService auditLedgerService;

    public DjeApplicationService(DjePublicacaoService djePublicacaoService,
                                 DjeProperties djeProperties,
                                 AuditLedgerService auditLedgerService) {
        this.djePublicacaoService = Objects.requireNonNull(djePublicacaoService);
        this.djeProperties = Objects.requireNonNull(djeProperties);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public DjePublicationMetricsView metrics() {
        return djePublicacaoService.publicationMetrics();
    }

    @Transactional
    public DjeLifecycleExecutionSummary lifecycleRun(LocalDate hoje, Integer batchSize) {
        LocalDate effectiveHoje = hoje == null ? LocalDate.now() : hoje;
        int effectiveBatch = batchSize == null || batchSize <= 0 ? djeProperties.maxBatchSize() : batchSize;
        DjeLifecycleExecutionSummary summary = djePublicacaoService.executarLifecycle(effectiveHoje, effectiveBatch);
        auditLedgerService.appendSafely(
                "DJE_LIFECYCLE_RUN",
                "DJE",
                effectiveHoje.toString(),
                null,
                "publicadas=" + summary.publicadasConsolidadas() + " notificadas=" + summary.partesNotificadas() + " batchSize=" + effectiveBatch);
        return summary;
    }

    @Transactional(readOnly = true)
    public DjePublicacaoConsultaResult publication(Long djeId) {
        return djePublicacaoService.consultar(new DjePublicacaoConsultaCommand(requireId(djeId)));
    }

    @Transactional(readOnly = true)
    public DjeStatusResult status(Long djeId) {
        return djePublicacaoService.status(new DjeStatusQuery(requireId(djeId)));
    }

    @Transactional(readOnly = true)
    public DjeHealthResult health(Long djeId) {
        return djePublicacaoService.health(new DjeHealthQuery(requireId(djeId)));
    }

    @Transactional(readOnly = true)
    public DjeLifecycleHealthView lifecycleHealth(Long djeId) {
        return djePublicacaoService.lifecycleHealth(requireId(djeId));
    }

    @Transactional(readOnly = true)
    public DjeTimelineView timeline(Long djeId) {
        Long requiredId = requireId(djeId);
        DjeTimelineView view = djePublicacaoService.timeline(new DjeTimelineQuery(requiredId));
        auditLedgerService.appendSafely("DJE_TIMELINE_QUERY", "DJE", String.valueOf(requiredId), null, "entries=" + view.entries().size());
        return view;
    }

    @Transactional(readOnly = true)
    public DjeConsultaPrazoResult prazo(Long djeId) {
        return djePublicacaoService.consultarPrazo(new DjeConsultaPrazoCommand(requireId(djeId)));
    }

    @Transactional(readOnly = true)
    public DjePrazoHealthResult prazoHealth(Long djeId) {
        return djePublicacaoService.prazoHealth(new DjePrazoHealthQuery(requireId(djeId)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.dje.domain.DjeDeadlineWindowView deadlineWindow(Long djeId) {
        return djePublicacaoService.deadlineWindow(requireId(djeId));
    }

    @Transactional(readOnly = true)
    public DjeNotificacaoResult notificacao(Long djeId) {
        return djePublicacaoService.notificacao(new DjeNotificacaoQuery(requireId(djeId)));
    }

    @Transactional(readOnly = true)
    public DjeNotificationAuditView notificacaoAudit(Long djeId) {
        return djePublicacaoService.notificationAuditView(requireId(djeId));
    }

    @Transactional(readOnly = true)
    public DjeFalhaResult falha(Long djeId) {
        return djePublicacaoService.falha(new DjeFalhaQuery(requireId(djeId)));
    }

    @Transactional(readOnly = true)
    public DjePublicationConsistencyView consistency(Long djeId) {
        return djePublicacaoService.consistencyView(requireId(djeId));
    }

    @Transactional(readOnly = true)
    public DjePublicationConsistencyAuditView consistencyAudit(Long djeId) {
        return djePublicacaoService.consistencyAudit(requireId(djeId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.dje.domain.DjeConteudoHealthView conteudoHealth(Long djeId) {
        return djePublicacaoService.conteudoHealthView(requireId(djeId));
    }

    @Transactional(readOnly = true)
    public DjeSignalView signal(Long djeId) {
        return djePublicacaoService.signalView(requireId(djeId));
    }

    @Transactional(readOnly = true)
    public DjeOwnerView owner(Long djeId) {
        return djePublicacaoService.ownerView(requireId(djeId));
    }

    @Transactional(readOnly = true)
    public DjeWindowView window(Long djeId) {
        return djePublicacaoService.windowView(requireId(djeId));
    }

    @Transactional(readOnly = true)
    public DjeAuditEntryView audit(Long djeId) {
        return djePublicacaoService.auditEntryView(requireId(djeId));
    }

    @Transactional(readOnly = true)
    public DjeProcessoPublicationView processoPublication(Long processoId) {
        return djePublicacaoService.processoPublicationView(requireId(processoId));
    }

    @Transactional(readOnly = true)
    public DjeTribunalPublicationResult tribunalPublication(String tribunalCodigo) {
        String normalized = normalizeReference(tribunalCodigo, "tribunalCodigo obrigatorio");
        return djePublicacaoService.tribunalPublication(new DjeTribunalPublicationQuery(normalized, null, null));
    }

    @Transactional(readOnly = true)
    public DjeTribunalHealthView tribunalHealth(String tribunalCodigo) {
        String normalized = normalizeReference(tribunalCodigo, "tribunalCodigo obrigatorio");
        DjeTribunalHealthView view = djePublicacaoService.tribunalHealthView(normalized);
        auditLedgerService.appendSafely("DJE_TRIBUNAL_HEALTH_QUERY", "DJE_TRIBUNAL", normalized, null, view.summary());
        return view;
    }

    @Transactional(readOnly = true)
    public DjeBudgetView budget(String tribunalCodigo) {
        return djePublicacaoService.budgetView(normalizeReference(tribunalCodigo, "tribunalCodigo obrigatorio"));
    }

    @Transactional(readOnly = true)
    public DjeDispatchHealthResult dispatchHealth(String tribunalCodigo, int limit) {
        return djePublicacaoService.dispatchHealth(new DjeDispatchHealthQuery(normalizeReference(tribunalCodigo, "tribunalCodigo obrigatorio"), Math.max(1, limit)));
    }

    @Transactional(readOnly = true)
    public DjeDispatchWindowView dispatchWindow(String tribunalCodigo, int limit) {
        return djePublicacaoService.dispatchWindow(normalizeReference(tribunalCodigo, "tribunalCodigo obrigatorio"), Math.max(1, limit));
    }

    @Transactional(readOnly = true)
    public DjePublicationWindowHealthView publicationWindowHealth(String tribunalCodigo) {
        return djePublicacaoService.publicationWindowHealthView(normalizeReference(tribunalCodigo, "tribunalCodigo obrigatorio"));
    }

    @Transactional(readOnly = true)
    public DjeEdicaoMetricsView edicaoMetrics(String edicao) {
        String normalized = normalizeReference(edicao, "edicao obrigatoria");
        DjeEdicaoMetricsView view = djePublicacaoService.edicaoMetrics(normalized);
        auditLedgerService.appendSafely("DJE_EDICAO_METRICS_QUERY", "DJE_EDICAO", normalized, null, "publicacoes=" + view.publicacoes());
        return view;
    }

    @Transactional(readOnly = true)
    public DjeEdicaoHealthSnapshot edicaoHealth(String edicao) {
        return djePublicacaoService.edicaoHealth(normalizeReference(edicao, "edicao obrigatoria"));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.dje.domain.DjeTribunalHealthResult tribunalHealthResult(String tribunalCodigo, String criterio) {
        return djePublicacaoService.tribunalHealth(new DjeTribunalHealthQuery(
                normalizeReference(tribunalCodigo, "tribunalCodigo obrigatorio"),
                criterio == null || criterio.isBlank() ? "publicacao" : criterio.trim().toLowerCase(Locale.ROOT),
                Instant.now()));
    }

    private Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id obrigatorio");
        }
        return id;
    }

    private String normalizeReference(String value, String message) {
        String normalized = Objects.requireNonNull(value, message).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
