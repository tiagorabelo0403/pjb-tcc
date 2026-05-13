package com.tcc.pjb.backend.core.observability.systemhealth;

import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PjbFunctionalAvailabilityHealthIndicator implements HealthIndicator {

    private final PjbFunctionalAvailabilityService service;

    public PjbFunctionalAvailabilityHealthIndicator(PjbFunctionalAvailabilityService service) {
        this.service = service;
    }

    @Override
    public Health health() {
        boolean up = true;
        for (PjbFunctionalDomain domain : PjbFunctionalDomain.values()) {
            if (!service.readiness(domain).available()) {
                up = false;
                break;
            }
        }
        Health.Builder builder = up ? Health.up() : Health.status("DEGRADED");
        for (Map.Entry<String, Object> entry : service.exportReadiness().entrySet()) {
            builder.withDetail(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}
