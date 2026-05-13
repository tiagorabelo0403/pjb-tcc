package com.tcc.pjb.backend.service.recursal.mesh;

import java.util.Locale;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshIndexDriftReport;

@Component
public class RecursalMeshIndexDriftHealthIndicator implements HealthIndicator {

    private final RecursalMeshIndexDriftService driftService;

    public RecursalMeshIndexDriftHealthIndicator(RecursalMeshIndexDriftService driftService) {
        this.driftService = driftService;
    }

    @Override
    public Health health() {
        RecursalMeshIndexDriftReport report = driftService.assess(50);
        Health.Builder builder = switch (report.severity().toUpperCase(Locale.ROOT)) {
            case "HEALTHY" -> Health.up();
            case "MONITOR" -> Health.status("DEGRADED");
            case "HIGH" -> Health.down();
            case "CRITICAL" -> Health.outOfService();
            default -> Health.unknown();
        };
        return builder
                .withDetail("indexName", report.indexName())
                .withDetail("projectionCount", report.projectionCount())
                .withDetail("indexCount", report.indexCount())
                .withDetail("sampled", report.sampled())
                .withDetail("missingInIndex", report.missingInIndex())
                .withDetail("outdatedInIndex", report.outdatedInIndex())
                .withDetail("divergentState", report.divergentState())
                .withDetail("divergentRevision", report.divergentRevision())
                .withDetail("severity", report.severity())
                .build();
    }
}
