package com.tcc.pjb.backend.service.outbox;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record FederatedEventEnvelope(
        String envelopeId,
        String eventCode,
        String sourceSystem,
        String sourceTribunal,
        String aggregateType,
        String aggregateId,
        String causalityKey,
        String idempotencyKey,
        Long aggregateVersion,
        Instant sourceOccurredAt,
        Instant ingestedAt,
        Map<String, Object> attributes,
        Object payload
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("envelopeId", envelopeId);
        out.put("eventCode", eventCode);
        out.put("sourceSystem", sourceSystem);
        out.put("sourceTribunal", sourceTribunal);
        out.put("aggregateType", aggregateType);
        out.put("aggregateId", aggregateId);
        out.put("causalityKey", causalityKey);
        out.put("idempotencyKey", idempotencyKey);
        out.put("aggregateVersion", aggregateVersion);
        out.put("sourceOccurredAt", sourceOccurredAt != null ? sourceOccurredAt.toString() : null);
        out.put("ingestedAt", ingestedAt != null ? ingestedAt.toString() : null);
        out.put("attributes", attributes == null ? Map.of() : attributes);
        out.put("payload", payload);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
