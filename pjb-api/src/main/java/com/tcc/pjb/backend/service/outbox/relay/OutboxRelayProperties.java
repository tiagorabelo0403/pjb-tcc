package com.tcc.pjb.backend.service.outbox.relay;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pjb.outbox.relay")
public record OutboxRelayProperties(
        boolean enabled,
        @DefaultValue("50") int batchSize,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("5000") long sendTimeoutMs,
        @DefaultValue("5s") Duration retryBackoff
) {
    public OutboxRelayProperties {
        batchSize = Math.max(1, batchSize);
        maxAttempts = Math.max(1, maxAttempts);
        sendTimeoutMs = Math.max(1000L, sendTimeoutMs);
    }
}
