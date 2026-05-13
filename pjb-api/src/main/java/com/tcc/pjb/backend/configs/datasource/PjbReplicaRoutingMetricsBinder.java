package com.tcc.pjb.backend.configs.datasource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.datasource.routing", name = "enabled", havingValue = "true")
public class PjbReplicaRoutingMetricsBinder implements MeterBinder {

    private final PjbReplicaObservationService observationService;

    public PjbReplicaRoutingMetricsBinder(PjbReplicaObservationService observationService) {
        this.observationService = observationService;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("pjb.datasource.replica.default.available", this,
                        binder -> binder.defaultReplicaAvailable())
                .register(registry);
        Gauge.builder("pjb.datasource.replica.default.replay.lag.seconds", this,
                        binder -> binder.defaultReplicaLagSeconds())
                .register(registry);
        Gauge.builder("pjb.datasource.replica.regional.configured", this,
                        binder -> binder.snapshot().configuredRegionalReplicas())
                .register(registry);
        Gauge.builder("pjb.datasource.replica.regional.available", this,
                        binder -> binder.snapshot().availableRegionalReplicas())
                .register(registry);
    }

    private double defaultReplicaAvailable() {
        PjbReplicaObservationService.ReplicaNodeSnapshot read = snapshot().read();
        return read != null && read.available() ? 1D : 0D;
    }

    private double defaultReplicaLagSeconds() {
        PjbReplicaObservationService.ReplicaNodeSnapshot read = snapshot().read();
        if (read == null || read.replayLagSeconds() == null) {
            return 0D;
        }
        return Math.max(0D, read.replayLagSeconds());
    }

    private PjbReplicaObservationService.ReplicaObservationSnapshot snapshot() {
        return observationService.currentSnapshot();
    }
}
