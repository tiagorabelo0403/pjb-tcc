package com.tcc.pjb.backend.core.kernel.process.events;

import java.time.Instant;

public record ProcessEventAppendedEvent(
        Long processoId,
        Long seq,
        String eventType,
        String payloadJson,
        String payloadHash,
        String prevChainHash,
        String chainHash,
        Instant createdAt,
        Long actorUserId,
        String actorRole
) {
}
