package com.tcc.pjb.backend.integration.judicial.security;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record JudicialSecureHttpResponse(
        int statusCode,
        Map<String, List<String>> headers,
        byte[] body,
        Instant receivedAt
) {
    public JudicialSecureHttpResponse {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? null : body.clone();
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }

    public String bodyAsString() {
        return bodyAsString(StandardCharsets.UTF_8);
    }

    public String bodyAsString(Charset charset) {
        return body == null ? null : new String(body, charset);
    }
}
