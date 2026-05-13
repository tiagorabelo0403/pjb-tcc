package com.tcc.pjb.backend.configs.datasource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class PjbReplicaFailoverTracker {

    private final Duration failureCooldown;
    private final boolean fallbackToWriteOnError;
    private final AtomicLong unavailableUntilEpochMilli = new AtomicLong(0L);
    private final AtomicLong fallbackCount = new AtomicLong(0L);
    private final AtomicLong readSuccessCount = new AtomicLong(0L);
    private final AtomicLong readFailureCount = new AtomicLong(0L);

    public PjbReplicaFailoverTracker(Duration failureCooldown,
                                     boolean fallbackToWriteOnError,
                                     MeterRegistry meterRegistry) {
        this.failureCooldown = failureCooldown == null || failureCooldown.isNegative() || failureCooldown.isZero()
                ? Duration.ofSeconds(30)
                : failureCooldown;
        this.fallbackToWriteOnError = fallbackToWriteOnError;
        if (meterRegistry != null) {
            Gauge.builder("pjb.datasource.replica.unavailable", this, tracker -> tracker.isReplicaAvailable() ? 0 : 1)
                    .register(meterRegistry);
            Gauge.builder("pjb.datasource.replica.fallback.count", fallbackCount, AtomicLong::get)
                    .register(meterRegistry);
            Gauge.builder("pjb.datasource.replica.success.count", readSuccessCount, AtomicLong::get)
                    .register(meterRegistry);
            Gauge.builder("pjb.datasource.replica.failure.count", readFailureCount, AtomicLong::get)
                    .register(meterRegistry);
        }
    }

    public boolean isFallbackToWriteOnError() {
        return fallbackToWriteOnError;
    }

    public boolean isReplicaAvailable() {
        return unavailableUntilEpochMilli.get() <= System.currentTimeMillis();
    }

    public void recordReplicaSuccess() {
        readSuccessCount.incrementAndGet();
    }

    public void recordReplicaFailure() {
        readFailureCount.incrementAndGet();
        fallbackCount.incrementAndGet();
        unavailableUntilEpochMilli.set(System.currentTimeMillis() + failureCooldown.toMillis());
    }

    public Instant unavailableUntil() {
        long value = unavailableUntilEpochMilli.get();
        return value <= 0L ? Instant.EPOCH : Instant.ofEpochMilli(value);
    }

    public Duration failureCooldown() {
        return failureCooldown;
    }

    public long fallbackCount() {
        return fallbackCount.get();
    }

    public long readSuccessCount() {
        return readSuccessCount.get();
    }

    public long readFailureCount() {
        return readFailureCount.get();
    }
}
