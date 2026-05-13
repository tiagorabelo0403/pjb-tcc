package com.tcc.pjb.backend.core.security.edge;

import java.time.Instant;
import java.util.List;

public record EdgeCryptoAttestation(
        String schema,
        String algoritmoHash,
        String hashSha384,
        long tamanhoBytes,
        Instant signedAt,
        String signerSubject,
        List<String> certChainPem,
        String signatureBase64
) {
}
