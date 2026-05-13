package com.tcc.pjb.backend.configs.datasource;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class PjbReplicaRoutingHealthIndicator implements HealthIndicator {

    private final PjbReplicaFailoverTracker tracker;
    private final PjbReplicaObservationService observationService;

    public PjbReplicaRoutingHealthIndicator(PjbReplicaFailoverTracker tracker,
                                            PjbReplicaObservationService observationService) {
        this.tracker = tracker;
        this.observationService = observationService;
    }

    @Override
    public Health health() {
        PjbReplicaObservationService.ReplicaObservationSnapshot snapshot = observationService.currentSnapshot();
        boolean defaultReplicaHealthy = snapshot.read() != null && snapshot.read().available();
        boolean statusUp = tracker.isReplicaAvailable() && defaultReplicaHealthy;
        Health.Builder builder = statusUp ? Health.up() : Health.outOfService();
        return builder
                .withDetail("replicaAvailable", tracker.isReplicaAvailable())
                .withDetail("unavailableUntil", tracker.unavailableUntil())
                .withDetail("failureCooldown", tracker.failureCooldown())
                .withDetail("fallbackCount", tracker.fallbackCount())
                .withDetail("readSuccessCount", tracker.readSuccessCount())
                .withDetail("readFailureCount", tracker.readFailureCount())
                .withDetail("observedAt", snapshot.observedAt())
                .withDetail("defaultReplicaLagSeconds", snapshot.read() == null ? null : snapshot.read().replayLagSeconds())
                .withDetail("defaultReplicaInRecovery", snapshot.read() == null ? null : snapshot.read().inRecovery())
                .withDetail("defaultReplicaUrl", snapshot.read() == null ? null : snapshot.read().jdbcUrl())
                .withDetail("configuredRegionalReplicas", snapshot.configuredRegionalReplicas())
                .withDetail("availableRegionalReplicas", snapshot.availableRegionalReplicas())
                .withDetail("regionalReplicas", snapshot.regional())
                .build();
    }
}
