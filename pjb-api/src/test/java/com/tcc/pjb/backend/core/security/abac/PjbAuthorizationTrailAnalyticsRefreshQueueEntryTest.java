package com.tcc.pjb.backend.core.security.abac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PjbAuthorizationTrailAnalyticsRefreshQueueEntryTest {

    @Test
    void mustKeepBucketQueuedWhenNewEventArrivesDuringProcessing() {
        Instant now = Instant.parse("2026-04-04T20:00:00Z");
        PjbAuthorizationTrailAnalyticsRefreshQueueEntry entry = PjbAuthorizationTrailAnalyticsRefreshQueueEntry.of(
                PjbAuthorizationTrailTemporalGranularity.HOUR,
                now,
                now.plusSeconds(3600),
                now
        );
        entry.setStatus(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PROCESSING.name());
        entry.markEnqueued(now.plusSeconds(30));
        assertTrue(entry.isRequeueRequested());
        entry.markCompleted(now.plusSeconds(60));
        assertEquals(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PENDING.name(), entry.getStatus());
        assertFalse(entry.isRequeueRequested());
    }

    @Test
    void mustBackoffWhenMarkedAsFailed() {
        Instant now = Instant.parse("2026-04-04T20:00:00Z");
        PjbAuthorizationTrailAnalyticsRefreshQueueEntry entry = PjbAuthorizationTrailAnalyticsRefreshQueueEntry.of(
                PjbAuthorizationTrailTemporalGranularity.DAY,
                now,
                now.plusSeconds(86400),
                now
        );
        entry.setAttemptCount(3);
        entry.markFailed(now, "erro transitório");
        assertEquals(PjbAuthorizationTrailAnalyticsRefreshQueueStatus.FAILED.name(), entry.getStatus());
        assertTrue(entry.nextVisibleAtInstant().isAfter(now));
    }
}
