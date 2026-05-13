package com.tcc.pjb.backend.platform.security.idempotency;

import com.tcc.pjb.backend.core.i18n.PjbPlatformMessageCatalog;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyAuditEntryView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyAuditView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyBudgetHealthView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyBudgetView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyConsistencyView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyDecisionView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyEnvelopeView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyExecutionHealthView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyHealthSnapshot;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeyHealthView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeyQuery;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeyResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeySnapshot;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyOwnerView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayHealthResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayPayload;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayQuery;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencySignalView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyStatusHealthResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyTimelineResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyWindowQuery;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyWindowResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyWindowView;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbIdempotencyApplicationService {

    private final PjbIdempotencyService idempotencyService;
    private final PjbIdempotencyPolicy policy;
    private final PjbPlatformMessageCatalog messages;
    private final AuditLedgerService auditLedgerService;

    public PjbIdempotencyApplicationService(PjbIdempotencyService idempotencyService,
                                            PjbIdempotencyPolicy policy,
                                            PjbPlatformMessageCatalog messages,
                                            AuditLedgerService auditLedgerService) {
        this.idempotencyService = Objects.requireNonNull(idempotencyService);
        this.policy = Objects.requireNonNull(policy);
        this.messages = Objects.requireNonNull(messages);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyKeyResult key(String key) {
        return idempotencyService.query(new PjbIdempotencyKeyQuery(normalizeKey(key)));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyKeySnapshot snapshot(String key) {
        return idempotencyService.snapshot(normalizeKey(key));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyReplayView replayView(String key) {
        return idempotencyService.replayView(normalizeKey(key));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyTimelineResult timeline(String key) {
        PjbIdempotencyTimelineResult result = idempotencyService.timeline(normalizeKey(key));
        auditLedgerService.appendSafely("IDEMPOTENCY_TIMELINE_QUERY", "IDEMPOTENCY", normalizeKey(key), null, "entries=" + result.entries().size());
        return result;
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyWindowResult window(String key) {
        return idempotencyService.window(new PjbIdempotencyWindowQuery(normalizeKey(key)));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyAuditView audit(String key) {
        return idempotencyService.auditView(normalizeKey(key));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyDecisionView decision(String key) {
        return idempotencyService.decisionView(normalizeKey(key));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyReplayResult replay(String key) {
        return idempotencyService.replay(new PjbIdempotencyReplayQuery(normalizeKey(key)));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyKeyHealthView keyHealth(String key) {
        return idempotencyService.keyHealthView(normalizeKey(key));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyHealthSnapshot health() {
        return idempotencyService.healthSnapshot();
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyExecutionHealthView executionHealth(String key, String route) {
        String status = idempotencyService.status(normalizeKey(key));
        boolean healthy = status == null || !"PROCESSING".equals(status) || route != null;
        return new PjbIdempotencyExecutionHealthView(normalizeKey(key), status == null ? "UNKNOWN" : status, healthy, normalizeRoute(route));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyStatusHealthResult statusHealth(String key) {
        String status = idempotencyService.status(normalizeKey(key));
        return new PjbIdempotencyStatusHealthResult(true, status == null ? messages.text("pjb.idempotency.key.required") : messages.text("pjb.offline.governance.detail", status), status == null ? 0L : 1L);
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyReplayHealthResult replayHealth(String key) {
        boolean ready = idempotencyService.loadReplay(normalizeKey(key)).isPresent();
        return new PjbIdempotencyReplayHealthResult(true, ready ? messages.text("pjb.offline.timeline.detail", "replay pronto") : messages.text("pjb.offline.timeline.detail", "replay ausente"), ready ? 1L : 0L);
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyBudgetView budget() {
        return new PjbIdempotencyBudgetView("IDEMPOTENCY", "OK", messages.text("pjb.idempotency.window.summary", policy.ttl().toHours(), policy.retryAfterSeconds()));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyBudgetHealthView budgetHealth() {
        return new PjbIdempotencyBudgetHealthView(policy.ttl(), policy.retryAfterSeconds(), true, messages.text("pjb.idempotency.window.configured"));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyConsistencyView consistency(String key) {
        String status = idempotencyService.status(normalizeKey(key));
        boolean consistent = status == null || "PROCESSING".equals(status) || "OK".equals(status);
        return new PjbIdempotencyConsistencyView(normalizeKey(key), nullSafeStatus(key), consistent, consistent ? messages.text("pjb.idempotency.consistency.recognized") : messages.text("pjb.idempotency.consistency.unrecognized"));
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyEnvelopeView envelope(String key) {
        return new PjbIdempotencyEnvelopeView(normalizeKey(key), nullSafeStatus(key), envelopeDetail(key), Instant.now(), null);
    }

    @Transactional(readOnly = true)
    public PjbIdempotencySignalView signal(String key) {
        return new PjbIdempotencySignalView(normalizeKey(key), nullSafeStatus(key), signalDetail(key), Instant.now(), 1L);
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyOwnerView owner(String key) {
        return new PjbIdempotencyOwnerView(normalizeKey(key), nullSafeStatus(key), "header=Idempotency-Key", Instant.now(), 1L);
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyWindowView windowView(String key) {
        PjbIdempotencyWindowResult result = window(key);
        return new PjbIdempotencyWindowView(result.key(), result.active() ? "ACTIVE" : "INACTIVE", "retryAfter=" + result.retryAfterSeconds(), Instant.now(), 1L);
    }

    @Transactional(readOnly = true)
    public PjbIdempotencyAuditEntryView auditEntry(String key) {
        return new PjbIdempotencyAuditEntryView(normalizeKey(key), nullSafeStatus(key), envelopeDetail(key), Instant.now(), 1L);
    }

    @Transactional
    public PjbIdempotencyKeySnapshot release(String key) {
        String normalized = normalizeKey(key);
        idempotencyService.release(normalized);
        auditLedgerService.appendSafely("IDEMPOTENCY_MANUAL_RELEASE", "IDEMPOTENCY", normalized, null, "manual=true");
        return idempotencyService.snapshot(normalized);
    }

    private String nullSafeStatus(String key) {
        String status = idempotencyService.status(normalizeKey(key));
        return status == null ? "UNKNOWN" : status;
    }

    private String envelopeDetail(String key) {
        return idempotencyService.loadReplay(normalizeKey(key))
                .map(PjbIdempotencyReplayPayload::status)
                .map(status -> "httpStatus=" + status)
                .orElse("sem replay");
    }

    private String signalDetail(String key) {
        String status = idempotencyService.status(normalizeKey(key));
        return "PROCESSING".equals(status) ? "retry-after=" + policy.retryAfterSeconds() : envelopeDetail(key);
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(messages.text("pjb.idempotency.key.required"));
        }
        return key.trim();
    }

    private String normalizeRoute(String route) {
        if (route == null || route.isBlank()) {
            return "UNSPECIFIED";
        }
        return route.trim().toUpperCase(Locale.ROOT);
    }
}
