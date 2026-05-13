package com.tcc.pjb.backend.service.rito;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;






@ConfigurationProperties(prefix = "pjb.ritos.governance")
public record RitoGovernanceProperties(
        
        int dualApprovalMinOccurrences,
        
        List<String> criticalRamos
) {
    public RitoGovernanceProperties {
        if (dualApprovalMinOccurrences <= 0) dualApprovalMinOccurrences = 10;
        if (criticalRamos == null || criticalRamos.isEmpty()) {
            criticalRamos = List.of("PENAL", "ELEITORAL", "MILITAR", "INFANCIA");
        }
    }
}
