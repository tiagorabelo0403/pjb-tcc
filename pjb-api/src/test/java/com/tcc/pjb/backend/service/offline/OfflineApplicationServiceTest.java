package com.tcc.pjb.backend.service.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.i18n.PjbPlatformMessageCatalog;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleTimelineHealthView;
import com.tcc.pjb.backend.service.offline.domain.OfflineConflictTimelineEntry;
import com.tcc.pjb.backend.service.offline.domain.OfflineConflictTimelineResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineGovernanceAuditView;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfflineApplicationServiceTest {

    @Test
    void conflictTimeline_deveAuditarQuantidadeDeEntradas() {
        PwaOfflineService offlineService = mock(PwaOfflineService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(offlineService.conflictTimeline("PWA-ABC")).thenReturn(new OfflineConflictTimelineResult(
                "PWA-ABC",
                List.of(
                        new OfflineConflictTimelineEntry("ABERTO", Instant.parse("2026-04-11T10:00:00Z"), "ok"),
                        new OfflineConflictTimelineEntry("CONFLITO", Instant.parse("2026-04-11T10:05:00Z"), "review"))));
        OfflineApplicationService applicationService = new OfflineApplicationService(offlineService, auditLedgerService, new PjbPlatformMessageCatalog());

        var result = applicationService.conflictTimeline("pwa-abc");

        assertThat(result.entries()).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("OFFLINE_CONFLICT_TIMELINE_QUERY"), eq("OFFLINE"), eq("PWA-ABC"), isNull(), eq("referencia=PWA-ABC entries=2"));
    }

    @Test
    void governanceAudit_deveAuditarStatus() {
        PwaOfflineService offlineService = mock(PwaOfflineService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(offlineService.governanceAuditView("PWA-LOCK")).thenReturn(new OfflineGovernanceAuditView("PWA-LOCK", "PENDENTE_REVISAO", Instant.parse("2026-04-11T10:00:00Z")));
        OfflineApplicationService applicationService = new OfflineApplicationService(offlineService, auditLedgerService, new PjbPlatformMessageCatalog());

        var result = applicationService.governanceAudit("pwa-lock");

        assertThat(result.status()).isEqualTo("PENDENTE_REVISAO");
        verify(auditLedgerService).appendSafely(eq("OFFLINE_GOVERNANCE_AUDIT_QUERY"), eq("OFFLINE"), eq("PWA-LOCK"), isNull(), eq("status=PENDENTE_REVISAO"));
    }

    @Test
    void timelineHealth_deveAuditarResumo() {
        PwaOfflineService offlineService = mock(PwaOfflineService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(offlineService.timelineHealthView("PWA-900")).thenReturn(new OfflineBundleTimelineHealthView("PWA-900", "OK", "eventos=3"));
        OfflineApplicationService applicationService = new OfflineApplicationService(offlineService, auditLedgerService, new PjbPlatformMessageCatalog());

        var result = applicationService.timelineHealth("pwa-900");

        assertThat(result.summary()).isEqualTo("eventos=3");
        verify(auditLedgerService).appendSafely(eq("OFFLINE_TIMELINE_HEALTH_QUERY"), eq("OFFLINE"), eq("PWA-900"), isNull(), eq("eventos=3"));
    }
}
