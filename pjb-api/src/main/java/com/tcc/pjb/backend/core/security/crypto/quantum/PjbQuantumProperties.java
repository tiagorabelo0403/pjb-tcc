package com.tcc.pjb.backend.core.security.crypto.quantum;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.crypto.pqc")
public record PjbQuantumProperties(
        boolean enabled,
        PqcSignatureAlgorithm signatureAlgorithm
) {
    public PjbQuantumProperties {
        if (signatureAlgorithm == null) {
            signatureAlgorithm = PqcSignatureAlgorithm.ML_DSA_87;
        }
    }
}
