package com.tcc.pjb.backend.configs.datasource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PjbWriteFailoverTracker {

    private static final int MAX_TRACKED_ENDPOINTS = 64;

    private final Duration failureCooldown;
    private final Duration successStickiness;
    private final Clock clock;
    private final ConcurrentHashMap<String, Instant> unavailableUntil = new ConcurrentHashMap<>();
    private final AtomicLong failoverCount = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private volatile String preferredEndpoint;
    private volatile Instant preferredUntil = Instant.EPOCH;

    public PjbWriteFailoverTracker(Duration failureCooldown,
                                   Duration successStickiness,
                                   MeterRegistry meterRegistry) {
        this(failureCooldown, successStickiness, Clock.systemUTC(), meterRegistry);
    }

    PjbWriteFailoverTracker(Duration failureCooldown,
                            Duration successStickiness,
                            Clock clock,
                            MeterRegistry meterRegistry) {
        this.failureCooldown = normalizePositive(failureCooldown, Duration.ofSeconds(10));
        this.successStickiness = normalizePositive(successStickiness, Duration.ofSeconds(20));
        this.clock = Objects.requireNonNull(clock, "clock");
        if (meterRegistry != null) {
            Gauge.builder("pjb.datasource.write.failover.count", failoverCount, AtomicLong::get).register(meterRegistry);
            Gauge.builder("pjb.datasource.write.success.count", successCount, AtomicLong::get).register(meterRegistry);
            Gauge.builder("pjb.datasource.write.failure.count", failureCount, AtomicLong::get).register(meterRegistry);
        }
    }

    public boolean isEndpointAvailable(String endpoint) {
        String normalized = normalize(endpoint);
        if (normalized == null) {
            return false;
        }
        cleanupUnavailableEndpoints();
        Instant blockedUntil = unavailableUntil.get(normalized);
        if (blockedUntil == null) {
            return true;
        }
        Instant now = Instant.now(clock);
        if (blockedUntil.isAfter(now)) {
            return false;
        }
        unavailableUntil.remove(normalized, blockedUntil);
        return true;
    }

    public void recordSuccess(String endpoint) {
        String normalized = normalize(endpoint);
        if (normalized == null) {
            return;
        }
        successCount.incrementAndGet();
        unavailableUntil.remove(normalized);
        cleanupUnavailableEndpoints();
        preferredEndpoint = normalized;
        preferredUntil = Instant.now(clock).plus(successStickiness);
    }

    public void recordFailure(String endpoint) {
        String normalized = normalize(endpoint);
        if (normalized == null) {
            return;
        }
        failureCount.incrementAndGet();
        unavailableUntil.put(normalized, Instant.now(clock).plus(failureCooldown));
        cleanupUnavailableEndpoints();
        if (normalized.equals(preferredEndpoint)) {
            preferredUntil = Instant.EPOCH;
        }
    }

    public void recordFailover(String fromEndpoint, String toEndpoint) {
        String from = normalize(fromEndpoint);
        String to = normalize(toEndpoint);
        if (from == null || to == null || from.equals(to)) {
            return;
        }
        failoverCount.incrementAndGet();
    }

    public List<String> prioritize(String primaryEndpoint, List<String> candidates) {
        LinkedHashMap<String, Boolean> ordered = new LinkedHashMap<>();
        String preferred = preferredEndpoint();
        if (preferred != null) {
            ordered.put(preferred, Boolean.TRUE);
        }
        String primary = normalize(primaryEndpoint);
        if (primary != null) {
            ordered.put(primary, Boolean.TRUE);
        }
        if (candidates != null) {
            for (String candidate : candidates) {
                String normalized = normalize(candidate);
                if (normalized != null) {
                    ordered.put(normalized, Boolean.TRUE);
                }
            }
        }
        return new ArrayList<>(ordered.keySet());
    }

    public String preferredEndpoint() {
        String current = preferredEndpoint;
        if (current == null || current.isBlank()) {
            return null;
        }
        if (preferredUntil.isAfter(Instant.now(clock))) {
            return current;
        }
        return null;
    }

    private static Duration normalizePositive(Duration value, Duration fallback) {
        if (value == null || value.isZero() || value.isNegative()) {
            return fallback;
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void cleanupUnavailableEndpoints() {
        Instant now = Instant.now(clock);
        unavailableUntil.entrySet().removeIf(entry -> entry.getValue() == null || !entry.getValue().isAfter(now));
        int overflow = unavailableUntil.size() - MAX_TRACKED_ENDPOINTS;
        if (overflow <= 0) {
            return;
        }
        unavailableUntil.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(overflow)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(unavailableUntil::remove);
    }

    int trackedUnavailableEndpoints() {
        return unavailableUntil.size();
    }
}
