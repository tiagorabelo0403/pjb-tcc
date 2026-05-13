package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.time.Instant;
import java.util.Map;

public record JudicialSignatureResult(
        JudicialSystem system,
        String tribunalCodigo,
        String keyAlias,
        String algorithm,
        byte[] signature,
        boolean hardwareBacked,
        Instant signedAt,
        Map<String, Object> metadata
) {
    public JudicialSignatureResult {
        signature = signature == null ? null : signature.clone();
        signedAt = signedAt == null ? Instant.now() : signedAt;
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }
}
