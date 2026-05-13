package com.tcc.pjb.backend.model.dto.integration.n8n;

import java.time.Duration;
import java.util.List;

public record N8nIntegrationProfileResponse(
        boolean enabled,
        String tenant,
        String baseUrl,
        String dispatchPath,
        Duration requestTimeout,
        int maxPayloadBytes,
        boolean requireHttps,
        boolean allowLocalHttp,
        boolean signedInbound,
        boolean signedOutbound,
        List<String> operationalModes,
        List<String> hardeningTracks
) {
}
