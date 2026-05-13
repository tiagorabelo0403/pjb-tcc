package com.tcc.pjb.backend.core.security.crypto.quantum;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.crypto.pqc")
public record PjbQuantumProperties(
        boolean enabled,
        
        String signatureAlgorithm
) {
    public PjbQuantumProperties {
        if (signatureAlgorithm == null || signatureAlgorithm.isBlank()) {
            signatureAlgorithm = "DILITHIUM"; 
        }
    }
}
