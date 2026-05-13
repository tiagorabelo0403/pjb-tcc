package com.tcc.pjb.backend.core.events;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import com.tcc.pjb.backend.core.correlation.CausationId;
import com.tcc.pjb.backend.core.correlation.CorrelationId;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;

public record PjbEventMetadata(
        String eventId,
        String eventType,
        PjbModuleId moduleId,
        String aggregateType,
        String aggregateId,
        String correlationId,
        String causationId,
        Instant occurredAt,
        int schemaVersion,
        Set<String> tags
) {

    public PjbEventMetadata {
        eventId = requireText(eventId, "eventId");
        eventType = requireText(eventType, "eventType");
        moduleId = Objects.requireNonNull(moduleId, "moduleId");
        aggregateType = requireText(aggregateType, "aggregateType");
        aggregateId = requireText(aggregateId, "aggregateId");
        correlationId = requireText(correlationId, "correlationId");
        causationId = requireText(causationId, "causationId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be >= 1");
        }
        tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
    }

    public static PjbEventMetadata of(PjbModuleId moduleId,
                                      String eventType,
                                      String aggregateType,
                                      String aggregateId,
                                      CorrelationId correlationId,
                                      CausationId causationId,
                                      int schemaVersion,
                                      Set<String> tags) {
        return new PjbEventMetadata(
                UUID.randomUUID().toString(),
                eventType,
                moduleId,
                aggregateType,
                aggregateId,
                correlationId.value(),
                causationId.value(),
                Instant.now(),
                schemaVersion,
                tags == null ? Set.of() : tags
        );
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
