package com.tcc.pjb.backend.core.peticionamento.blackbox;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record PjbProtocolBlackBoxEntry(
        PjbProtocolBlackBoxEventType type,
        Instant occurredAt,
        String actorHash,
        String payloadHash,
        String connectorCode,
        Map<String, String> evidence
) {
    public PjbProtocolBlackBoxEntry {
        type = type == null ? PjbProtocolBlackBoxEventType.REQUEST_ACCEPTED : type;
        occurredAt = occurredAt == null ? Instant.EPOCH : occurredAt;
        actorHash = Objects.toString(actorHash, "").trim();
        payloadHash = Objects.toString(payloadHash, "").trim();
        connectorCode = Objects.toString(connectorCode, "").trim().toUpperCase();
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
