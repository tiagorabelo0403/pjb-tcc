package com.tcc.pjb.backend.platform.security.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.i18n.PjbPlatformMessageCatalog;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeySnapshot;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayPayload;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyTimelineEntry;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyTimelineResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PjbIdempotencyApplicationServiceTest {

    @Test
    void timeline_deveAuditarQuantidadeDeEntradas() {
        PjbIdempotencyService service = mock(PjbIdempotencyService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(service.timeline("idem-1")).thenReturn(new PjbIdempotencyTimelineResult(List.of(
                new PjbIdempotencyTimelineEntry("idem-1", "PROCESSING", Instant.parse("2026-04-11T10:00:00Z")),
                new PjbIdempotencyTimelineEntry("idem-1", "REPLAY_READY", Instant.parse("2026-04-11T10:00:01Z")))));
        PjbIdempotencyApplicationService applicationService = new PjbIdempotencyApplicationService(service, new PjbIdempotencyPolicy(Duration.ofHours(24), 5), new PjbPlatformMessageCatalog(), auditLedgerService);

        var result = applicationService.timeline("idem-1");

        assertThat(result.entries()).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("IDEMPOTENCY_TIMELINE_QUERY"), eq("IDEMPOTENCY"), eq("idem-1"), isNull(), eq("entries=2"));
    }

    @Test
    void budgetHealth_deveRefletirPolicy() {
        PjbIdempotencyApplicationService applicationService = new PjbIdempotencyApplicationService(mock(PjbIdempotencyService.class), new PjbIdempotencyPolicy(Duration.ofHours(24), 5), new PjbPlatformMessageCatalog(), mock(AuditLedgerService.class));

        var result = applicationService.budgetHealth();

        assertThat(result.retryAfterSeconds()).isEqualTo(5);
        assertThat(result.healthy()).isTrue();
    }

    @Test
    void release_deveAuditarOperacaoManual() {
        PjbIdempotencyService service = mock(PjbIdempotencyService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(service.snapshot("idem-release")).thenReturn(new PjbIdempotencyKeySnapshot("idem-release", null, Instant.parse("2026-04-11T10:00:00Z")));
        PjbIdempotencyApplicationService applicationService = new PjbIdempotencyApplicationService(service, new PjbIdempotencyPolicy(Duration.ofHours(24), 5), new PjbPlatformMessageCatalog(), auditLedgerService);

        var result = applicationService.release("idem-release");

        assertThat(result.key()).isEqualTo("idem-release");
        verify(service).release("idem-release");
        verify(auditLedgerService).appendSafely(eq("IDEMPOTENCY_MANUAL_RELEASE"), eq("IDEMPOTENCY"), eq("idem-release"), isNull(), eq("manual=true"));
    }
}
