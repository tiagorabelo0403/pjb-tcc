package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.util.Map;

public record JudicialSignatureRequest(
        JudicialSystem system,
        String tribunalCodigo,
        String keyStoreRef,
        String keyAlias,
        String algorithm,
        byte[] payload,
        String correlationId,
        Map<String, Object> metadata
) {
    public JudicialSignatureRequest {
        payload = payload == null ? null : payload.clone();
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }
}
