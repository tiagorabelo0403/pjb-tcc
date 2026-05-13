package com.tcc.pjb.backend.core.security.crypto.quantum;


public record PqcEvidence(
        String algorithm,
        String signatureB64,
        String publicKeyB64
) {}
