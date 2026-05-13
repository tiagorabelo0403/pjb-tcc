
package com.tcc.pjb.backend.service.infra;

import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.kafka", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "pjb.datasource.routing.processual-read-models", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PjbProcessualReadModelKafkaConsumer {

    private final PjbProcessualReadModelProjector projector;

    public PjbProcessualReadModelKafkaConsumer(PjbProcessualReadModelProjector projector) {
        this.projector = Objects.requireNonNull(projector, "projector");
    }

    @KafkaListener(containerFactory = "pjbKafkaListenerContainerFactory",
            topics = "${pjb.datasource.routing.processual-read-models.kafka-topic:pjb.processual.read-models.v1}",
            groupId = "grupo-pjb-processual-read-models")
    public void handle(@Payload Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        String eventType = asString(payload.get("eventType"));
        String aggregateType = asString(payload.get("aggregateType"));
        String aggregateId = asString(payload.get("aggregateId"));
        projector.ingest("KAFKA", eventType, aggregateType, aggregateId, payload);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
