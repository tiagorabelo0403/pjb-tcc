package com.tcc.pjb.backend.core.observability.systemhealth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

@Component
public class PjbElasticsearchHealthIndicator implements HealthIndicator {

    private final ObjectProvider<ElasticsearchOperations> operations;

    public PjbElasticsearchHealthIndicator(ObjectProvider<ElasticsearchOperations> operations) {
        this.operations = operations;
    }

    @Override
    public Health health() {
        ElasticsearchOperations ops = operations.getIfAvailable();
        if (ops == null) {
            return Health.unknown().withDetail("reason", "not configured").build();
        }
        try {
            boolean indexExists = ops.indexOps(IndexCoordinates.of("pjb-processos")).exists();
            return Health.up()
                    .withDetail("index", indexExists ? "ready" : "pending")
                    .build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
