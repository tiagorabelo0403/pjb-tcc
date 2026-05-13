package com.tcc.pjb.backend.query.consumer;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaNotificationEnvelope;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaNotificationDispatchService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaNotificationEventPublisher;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.kafka", name = "enabled", havingValue = "true")
public class OficialJusticaNotificationKafkaConsumer {

    private final OficialJusticaNotificationDispatchService dispatchService;

    public OficialJusticaNotificationKafkaConsumer(OficialJusticaNotificationDispatchService dispatchService) {
        this.dispatchService = Objects.requireNonNull(dispatchService);
    }

    @KafkaListener(containerFactory = "pjbKafkaListenerContainerFactory",
            topics = OficialJusticaNotificationEventPublisher.TOPIC,
            groupId = OficialJusticaNotificationEventPublisher.GROUP_ID)
    public void consume(OficialJusticaNotificationEnvelope envelope) {
        dispatchService.dispatch(envelope);
    }
}
