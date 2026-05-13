package com.tcc.pjb.backend.core.peticionamento.blackbox;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbProtocolBlackBoxEnvelope(
        String protocolIntentId,
        String processNumber,
        Instant openedAt,
        List<PjbProtocolBlackBoxEntry> entries,
        String chainHash,
        boolean sealed
) {
    public PjbProtocolBlackBoxEnvelope {
        protocolIntentId = Objects.toString(protocolIntentId, "").trim();
        processNumber = Objects.toString(processNumber, "").trim();
        openedAt = openedAt == null ? Instant.EPOCH : openedAt;
        entries = entries == null ? List.of() : List.copyOf(entries);
        chainHash = Objects.toString(chainHash, "").trim();
    }

    public boolean contains(PjbProtocolBlackBoxEventType type) {
        return type != null && entries.stream().anyMatch(entry -> entry.type() == type);
    }
}
