package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;

class PjbReplicaRoutingHealthIndicatorTest {

    @Test
    void shouldExposeLagAndRegionalCounts() {
        PjbReplicaFailoverTracker tracker = new PjbReplicaFailoverTracker(Duration.ofSeconds(30), true, null);
        PjbReplicaObservationService observationService = mock(PjbReplicaObservationService.class);
        when(observationService.currentSnapshot()).thenReturn(new PjbReplicaObservationService.ReplicaObservationSnapshot(
                Instant.now(),
                new PjbReplicaObservationService.ReplicaNodeSnapshot("WRITE", "jdbc:postgresql://primary/pjb", true, true, false, null, null),
                new PjbReplicaObservationService.ReplicaNodeSnapshot("READ", "jdbc:postgresql://replica/pjb", true, true, true, 1.75d, null),
                Map.of("READ_NORDESTE", new PjbReplicaObservationService.ReplicaNodeSnapshot("READ_NORDESTE", "jdbc:postgresql://ne/pjb", true, true, true, 0.8d, null)),
                true,
                2L,
                10L,
                1L,
                1L,
                1L
        ));
        PjbReplicaRoutingHealthIndicator indicator = new PjbReplicaRoutingHealthIndicator(tracker, observationService);

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("defaultReplicaLagSeconds", 1.75d);
        assertThat(health.getDetails()).containsEntry("availableRegionalReplicas", 1L);
    }
}
