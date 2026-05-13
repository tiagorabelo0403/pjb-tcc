package com.tcc.pjb.backend.platform.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PjbRuntimeDrainService {

    private final PjbRuntimeLifecycleProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean draining = new AtomicBoolean();
    private final AtomicLong drainingSinceEpochMillis = new AtomicLong();
    private volatile String reason = "startup";

    public PjbRuntimeDrainService(PjbRuntimeLifecycleProperties properties,
                                  ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        markAccepting("application-ready");
    }

    public boolean beginDrain(String newReason) {
        if (!properties.isEnabled()) {
            return false;
        }
        boolean changed = draining.compareAndSet(false, true);
        reason = normalizeReason(newReason, "draining");
        drainingSinceEpochMillis.compareAndSet(0L, System.currentTimeMillis());
        if (changed && properties.isPublishAvailabilityEvents()) {
            AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
        }
        return changed;
    }

    public void markAccepting(String newReason) {
        draining.set(false);
        drainingSinceEpochMillis.set(0L);
        reason = normalizeReason(newReason, "accepting-traffic");
        if (properties.isPublishAvailabilityEvents()) {
            AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
        }
    }

    public boolean isDraining() {
        return properties.isEnabled() && draining.get();
    }

    public boolean readyForTraffic() {
        return !properties.isFailReadyWhenDraining() || !isDraining();
    }

    public Instant drainingSince() {
        long timestamp = drainingSinceEpochMillis.get();
        return timestamp <= 0L ? null : Instant.ofEpochMilli(timestamp);
    }

    public long drainAgeMillis() {
        long timestamp = drainingSinceEpochMillis.get();
        if (timestamp <= 0L) {
            return 0L;
        }
        return Math.max(0L, System.currentTimeMillis() - timestamp);
    }

    public Duration drainQuietPeriod() {
        return sanitizeDuration(properties.getDrainQuietPeriod(), Duration.ofSeconds(20));
    }

    public Duration shutdownAwaitTimeout() {
        return sanitizeDuration(properties.getShutdownAwaitTimeout(), Duration.ofSeconds(30));
    }

    public String reason() {
        return reason;
    }

    public Snapshot snapshot() {
        return new Snapshot(isDraining(), readyForTraffic(), drainingSince(), drainAgeMillis(), drainQuietPeriod(), reason);
    }

    private String normalizeReason(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Duration sanitizeDuration(Duration value, Duration fallback) {
        if (value == null || value.isNegative() || value.isZero()) {
            return fallback;
        }
        return value;
    }

    public record Snapshot(boolean draining,
                           boolean readyForTraffic,
                           Instant drainingSince,
                           long drainAgeMillis,
                           Duration drainQuietPeriod,
                           String reason) {
    }
}
