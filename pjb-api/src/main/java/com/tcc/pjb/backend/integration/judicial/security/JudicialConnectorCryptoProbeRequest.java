package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.util.Map;

public record JudicialConnectorCryptoProbeRequest(
        JudicialSystem system,
        String tribunalCodigo,
        String targetUrl,
        String requestedBy,
        String correlationId,
        Map<String, Object> metadata
) {
    public JudicialConnectorCryptoProbeRequest {
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }
}
