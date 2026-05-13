package com.tcc.pjb.backend.configs.datasource;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PjbWriteFailoverTrackerTest {

    @Test
    void shouldTrimTrackedUnavailableEndpointsAndPruneExpiredOnRead() {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-09T12:00:00Z"));
        PjbWriteFailoverTracker tracker = new PjbWriteFailoverTracker(Duration.ofSeconds(5), Duration.ofSeconds(10), clock, new SimpleMeterRegistry());

        for (int i = 0; i < 100; i++) {
            tracker.recordFailure("endpoint-" + i);
        }

        assertTrue(tracker.trackedUnavailableEndpoints() <= 64);

        clock.advance(Duration.ofSeconds(6));
        tracker.isEndpointAvailable("endpoint-0");

        assertTrue(tracker.trackedUnavailableEndpoints() < 64);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
