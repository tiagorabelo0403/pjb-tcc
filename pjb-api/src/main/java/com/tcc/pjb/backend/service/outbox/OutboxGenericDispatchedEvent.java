package com.tcc.pjb.backend.service.outbox;

import java.time.Instant;

public record OutboxGenericDispatchedEvent(
    String eventType,
    String routingKey,
    String payloadJson,
    String headersJson,
    String aggregateType,
    String aggregateId,
    Instant createdAt
) {
}
