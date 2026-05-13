package com.tcc.pjb.backend.platform.runtime;

import com.tcc.pjb.backend.configs.live.LiveClusterStateStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class PjbLivePressureService {

    private final PjbRuntimePressureProperties properties;
    private final LiveClusterStateStore liveClusterStateStore;
    private final AtomicLong lastSubscriberCount = new AtomicLong();
    private final AtomicLong lastSampleAtEpochMillis = new AtomicLong();
    private final AtomicLong sustainedSinceEpochMillis = new AtomicLong();
    private final AtomicReference<Snapshot> lastSnapshot = new AtomicReference<>(new Snapshot(0L, 0L, 0L, false, false, false, false, false, List.of(), null));

    public PjbLivePressureService(PjbRuntimePressureProperties properties,
                                  LiveClusterStateStore liveClusterStateStore) {
        this.properties = properties;
        this.liveClusterStateStore = liveClusterStateStore;
    }

    public Snapshot snapshot(boolean warmingUp) {
        List<String> namespaces = namespaces();
        long totalSubscribers = 0L;
        long activeTopics = 0L;
        for (String namespace : namespaces) {
            totalSubscribers += Math.max(0L, liveClusterStateStore.totalSubscribers(namespace));
            activeTopics += Math.max(0L, liveClusterStateStore.activeTopics(namespace));
        }
        long now = System.currentTimeMillis();
        long previousSubscribers = lastSubscriberCount.getAndSet(totalSubscribers);
        long previousSampleAt = lastSampleAtEpochMillis.getAndSet(now);
        Duration trendWindow = sanitize(properties.getLiveTrendWindow(), Duration.ofSeconds(45));
        boolean previousFresh = previousSampleAt > 0L && now - previousSampleAt <= trendWindow.toMillis();
        long subscriberDelta = totalSubscribers - previousSubscribers;
        boolean degraded = properties.isEnabled() && !warmingUp && (totalSubscribers >= Math.max(1L, properties.getLiveTotalSubscribersThreshold())
                || activeTopics >= Math.max(1L, properties.getLiveActiveTopicsThreshold()));
        boolean risingFast = properties.isEnabled() && !warmingUp && previousFresh
                && subscriberDelta >= Math.max(1L, properties.getLiveSubscriberRisingFastDelta());
        if (degraded) {
            sustainedSinceEpochMillis.compareAndSet(0L, now);
        } else {
            sustainedSinceEpochMillis.set(0L);
        }
        long sustainedSince = sustainedSinceEpochMillis.get();
        boolean sustained = sustainedSince > 0L
                && now - sustainedSince >= sanitize(properties.getLiveSustainedWindow(), Duration.ofMinutes(2)).toMillis();
        boolean criticalSurge = properties.isEnabled() && !warmingUp && degraded
                && (risingFast || sustained || totalSubscribers >= Math.round(properties.getLiveTotalSubscribersThreshold() * 1.5d));
        Snapshot snapshot = new Snapshot(totalSubscribers, activeTopics, subscriberDelta, previousFresh, risingFast, sustained,
                degraded, criticalSurge, namespaces, sustainedSince > 0L ? Instant.ofEpochMilli(sustainedSince) : null);
        lastSnapshot.set(snapshot);
        return snapshot;
    }

    private List<String> namespaces() {
        List<String> values = new ArrayList<>();
        for (String value : properties.getLiveNamespaces()) {
            if (value != null && !value.isBlank()) {
                values.add(value.trim());
            }
        }
        if (!values.isEmpty()) {
            return List.copyOf(values);
        }
        return List.of("ui-history", "ui-accessibility", "ui-presentation", "secretariat", "julgamento-votos");
    }

    public Snapshot lastSnapshot() {
        return lastSnapshot.get();
    }

    private Duration sanitize(Duration value, Duration fallback) {
        if (value == null || value.isNegative() || value.isZero()) {
            return fallback;
        }
        return value;
    }

    public record Snapshot(long totalSubscribers,
                           long activeTopics,
                           long subscriberDelta,
                           boolean previousSampleFresh,
                           boolean risingFast,
                           boolean sustained,
                           boolean degraded,
                           boolean criticalSurge,
                           List<String> namespaces,
                           Instant sustainedSince) {
    }
}
