package com.tcc.pjb.backend.integration.judicial.security;

import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JudicialConnectorCertificatePostureScheduler {

    private final JudicialConnectorCertificateInventoryService inventoryService;

    public JudicialConnectorCertificatePostureScheduler(JudicialConnectorCertificateInventoryService inventoryService) {
        this.inventoryService = Objects.requireNonNull(inventoryService);
    }

    @Scheduled(
            initialDelayString = "${pjb.integration.judicial.security.posture.initial-delay-ms:30000}",
            fixedDelayString = "${pjb.integration.judicial.security.posture.fixed-delay-ms:900000}"
    )
    public void refreshInventory() {
        inventoryService.refreshConfiguredInventory();
    }
}
