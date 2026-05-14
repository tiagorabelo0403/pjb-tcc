package com.tcc.pjb.backend.core.events;

import java.time.Instant;
import java.util.UUID;

public record PjbIntegrationEventEnvelope(
        UUID uuid,
        String appName,
        String appVersion,
        String routingKey,
        Instant timestamp,
        String payloadHash,
        Object payload
) {
    public static PjbIntegrationEventEnvelope de(String routingKey, Object payload, String hash) {
        return new PjbIntegrationEventEnvelope(
                UUID.randomUUID(), "pjb", "1.0", routingKey, Instant.now(), hash, payload);
    }
}
