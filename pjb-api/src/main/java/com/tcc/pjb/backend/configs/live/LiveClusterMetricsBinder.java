package com.tcc.pjb.backend.configs.live;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LiveClusterMetricsBinder {

    private static final List<String> NAMESPACES = List.of(
        "ui-history",
        "ui-accessibility",
        "ui-presentation",
        "secretariat",
        "julgamento-votos"
    );

    public LiveClusterMetricsBinder(MeterRegistry meterRegistry, LiveClusterBus clusterBus, LiveClusterStateStore stateStore) {
        Gauge.builder("pjb_live_cluster_enabled", clusterBus, bus -> bus.enabled() ? 1.0d : 0.0d)
            .description("Live cluster bus enabled state")
            .register(meterRegistry);
        Gauge.builder("pjb_live_cluster_distributed_store_enabled", stateStore, store -> store.distributed() ? 1.0d : 0.0d)
            .description("Distributed live cluster state store enabled state")
            .register(meterRegistry);
        for (String namespace : NAMESPACES) {
            Gauge.builder("pjb_live_cluster_subscribers", stateStore, store -> (double) store.totalSubscribers(namespace))
                .description("Total live subscribers by namespace")
                .tag("namespace", namespace)
                .register(meterRegistry);
            Gauge.builder("pjb_live_cluster_topics", stateStore, store -> (double) store.activeTopics(namespace))
                .description("Active live topics by namespace")
                .tag("namespace", namespace)
                .register(meterRegistry);
        }
    }
}
