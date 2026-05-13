package com.tcc.pjb.backend.integration.judicial.security;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("judicialConnectorCrypto")
public class JudicialConnectorCryptoHealthIndicator implements HealthIndicator {

    private final JudicialConnectorCertificateInventoryService inventoryService;

    public JudicialConnectorCryptoHealthIndicator(JudicialConnectorCertificateInventoryService inventoryService) {
        this.inventoryService = Objects.requireNonNull(inventoryService);
    }

    @Override
    public Health health() {
        JudicialConnectorCryptoPostureSummary summary = inventoryService.postureSummary(Duration.ofHours(24));
        Health.Builder builder;
        if (summary.total() == 0) {
            builder = Health.unknown();
        } else if (summary.blocked() > 0 || summary.expired() > 0) {
            builder = Health.outOfService();
        } else if (summary.warning() > 0 || summary.expiringSoon() > 0 || summary.withRecentFailures() > 0) {
            builder = Health.status("DEGRADED");
        } else {
            builder = Health.up();
        }
        return builder
                .withDetail("summary", summary.toMap())
                .build();
    }
}
