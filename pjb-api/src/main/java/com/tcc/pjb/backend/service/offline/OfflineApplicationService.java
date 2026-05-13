package com.tcc.pjb.backend.service.offline;

import com.tcc.pjb.backend.core.i18n.PjbPlatformMessageCatalog;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.service.offline.domain.OfflineActionHealthView;
import com.tcc.pjb.backend.service.offline.domain.OfflineActionWindowView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleConsistencyView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleEnvelopeView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleExpiryView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleGovernanceStatusView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleMetricsView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleOwnershipView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleSignalView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleTimelineHealthView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleWindowView;
import com.tcc.pjb.backend.service.offline.domain.OfflineConflictTimelineResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineGovernanceAuditView;
import com.tcc.pjb.backend.service.offline.domain.OfflineManifestAuditView;
import com.tcc.pjb.backend.service.offline.domain.OfflineReplayAuditView;
import com.tcc.pjb.backend.service.offline.domain.OfflineReplayHealthView;
import com.tcc.pjb.backend.service.offline.domain.OfflineSyncDecisionView;
import com.tcc.pjb.backend.service.offline.domain.OfflineSyncWindowView;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfflineApplicationService {

    private final PwaOfflineService offlineService;
    private final AuditLedgerService auditLedgerService;
    private final PjbPlatformMessageCatalog messages;

    public OfflineApplicationService(PwaOfflineService offlineService,
                                     AuditLedgerService auditLedgerService,
                                     PjbPlatformMessageCatalog messages) {
        this.offlineService = Objects.requireNonNull(offlineService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.messages = Objects.requireNonNull(messages);
    }

    @Transactional(readOnly = true)
    public OfflineBundleMetricsView metrics(String bundleToken) {
        return offlineService.metricsView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineBundleGovernanceStatusView governanceStatus(String bundleToken) {
        return offlineService.governanceStatusView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineConflictTimelineResult conflictTimeline(String bundleToken) {
        OfflineConflictTimelineResult result = offlineService.conflictTimeline(normalizeToken(bundleToken));
        auditLedgerService.appendSafely("OFFLINE_CONFLICT_TIMELINE_QUERY", "OFFLINE", result.bundleToken(), null, messages.text("pjb.offline.timeline.reference", result.bundleToken()) + " entries=" + result.entries().size());
        return result;
    }

    @Transactional(readOnly = true)
    public OfflineBundleConsistencyView consistency(String bundleToken) {
        return offlineService.consistencyView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineBundleOwnershipView ownership(String bundleToken) {
        return offlineService.ownershipView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineBundleExpiryView expiry(String bundleToken) {
        return offlineService.expiryView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineBundleEnvelopeView envelope(String bundleToken) {
        return offlineService.envelopeView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineBundleSignalView signal(String bundleToken) {
        return offlineService.signalView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineBundleWindowView window(String bundleToken) {
        return offlineService.windowView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineSyncDecisionView decision(String bundleToken) {
        return offlineService.syncDecisionView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineReplayHealthView replayHealth(String bundleToken) {
        return offlineService.replayHealthView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineSyncWindowView syncWindow(String bundleToken) {
        return offlineService.syncWindowView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineActionHealthView actionHealth(String bundleToken) {
        return offlineService.actionHealthView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineActionWindowView actionWindow(String bundleToken) {
        return offlineService.actionWindowView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineManifestAuditView manifestAudit(String bundleToken) {
        return offlineService.manifestAuditView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineReplayAuditView replayAudit(String bundleToken) {
        return offlineService.replayAuditView(normalizeToken(bundleToken));
    }

    @Transactional(readOnly = true)
    public OfflineGovernanceAuditView governanceAudit(String bundleToken) {
        OfflineGovernanceAuditView view = offlineService.governanceAuditView(normalizeToken(bundleToken));
        auditLedgerService.appendSafely("OFFLINE_GOVERNANCE_AUDIT_QUERY", "OFFLINE", view.referencia(), null, messages.text("pjb.offline.governance.detail", view.status()));
        return view;
    }

    @Transactional(readOnly = true)
    public OfflineBundleTimelineHealthView timelineHealth(String bundleToken) {
        OfflineBundleTimelineHealthView view = offlineService.timelineHealthView(normalizeToken(bundleToken));
        auditLedgerService.appendSafely("OFFLINE_TIMELINE_HEALTH_QUERY", "OFFLINE", view.referencia(), null, messages.text("pjb.offline.timeline.detail", view.summary()));
        return view;
    }

    private String normalizeToken(String bundleToken) {
        if (bundleToken == null || bundleToken.isBlank()) {
            throw new IllegalArgumentException(messages.text("pjb.offline.bundle.required"));
        }
        return bundleToken.trim().toUpperCase(Locale.ROOT);
    }
}
