package com.tcc.pjb.backend.core.observability.systemhealth;

import java.util.List;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.service.rito.diagnostics.RitoPackStatus;

@Component
public class RitoPackHealthIndicator implements HealthIndicator {

    private final RitoPackStatus status;

    public RitoPackHealthIndicator(RitoPackStatus status) {
        this.status = status;
    }

    @Override
    public Health health() {
        if (!status.isLoaded()) {
            return Health.down()
                    .withDetail("component", "ritos_pack")
                    .withDetail("version", status.getVersion())
                    .withDetail("checksum", status.getChecksum())
                    .withDetail("loadedAt", status.getLoadedAt())
                    .withDetail("issues", status.getIssues())
                    .build();
        }

        List<String> issues = status.getIssues();
        if (issues != null && !issues.isEmpty()) {
            return Health.status(new Status("DEGRADED"))
                    .withDetail("component", "ritos_pack")
                    .withDetail("version", status.getVersion())
                    .withDetail("checksum", status.getChecksum())
                    .withDetail("loadedAt", status.getLoadedAt())
                    .withDetail("issueCount", issues.size())
                    .withDetail("sample", issues.stream().limit(10).toList())
                    .build();
        }

        return Health.up()
                .withDetail("component", "ritos_pack")
                .withDetail("version", status.getVersion())
                .withDetail("checksum", status.getChecksum())
                .withDetail("loadedAt", status.getLoadedAt())
                .build();
    }
}
