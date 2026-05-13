package com.tcc.pjb.backend.core.events;

import java.util.Objects;
import java.util.Set;
import com.tcc.pjb.backend.core.correlation.CausationId;
import com.tcc.pjb.backend.core.correlation.CorrelationId;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;

public record PjbEventEnvelope<T>(PjbEventMetadata metadata, T payload) {

    public PjbEventEnvelope {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(payload, "payload");
    }

    public static <T> PjbEventEnvelope<T> domain(PjbModuleId moduleId,
                                                 String eventType,
                                                 String aggregateType,
                                                 String aggregateId,
                                                 CorrelationId correlationId,
                                                 CausationId causationId,
                                                 int schemaVersion,
                                                 T payload,
                                                 Set<String> tags) {
        return new PjbEventEnvelope<>(
                PjbEventMetadata.of(
                        moduleId,
                        eventType,
                        aggregateType,
                        aggregateId,
                        correlationId,
                        causationId,
                        schemaVersion,
                        tags == null ? Set.of() : tags
                ),
                payload
        );
    }

    public static <T> PjbEventEnvelope<T> integration(PjbModuleId moduleId,
                                                      String eventType,
                                                      String aggregateType,
                                                      String aggregateId,
                                                      CorrelationId correlationId,
                                                      CausationId causationId,
                                                      int schemaVersion,
                                                      T payload,
                                                      Set<String> tags) {
        return domain(moduleId, eventType, aggregateType, aggregateId, correlationId, causationId, schemaVersion, payload, tags);
    }

    public String eventType() {
        return metadata.eventType();
    }

    public String moduleCode() {
        return metadata.moduleId().code();
    }
}
