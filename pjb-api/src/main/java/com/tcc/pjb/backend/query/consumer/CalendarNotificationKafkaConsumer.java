package com.tcc.pjb.backend.query.consumer;

import com.tcc.pjb.backend.model.dto.calendar.CalendarNotificationEnvelope;
import com.tcc.pjb.backend.service.calendar.CalendarNotificationDispatchService;
import com.tcc.pjb.backend.service.calendar.CalendarNotificationEventPublisher;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.kafka", name = "enabled", havingValue = "true")
public class CalendarNotificationKafkaConsumer {

    private final CalendarNotificationDispatchService dispatchService;

    public CalendarNotificationKafkaConsumer(CalendarNotificationDispatchService dispatchService) {
        this.dispatchService = Objects.requireNonNull(dispatchService);
    }

    @KafkaListener(containerFactory = "pjbKafkaListenerContainerFactory", topics = CalendarNotificationEventPublisher.TOPIC, groupId = "grupo-calendar-notification")
    public void consume(CalendarNotificationEnvelope envelope) {
        dispatchService.dispatch(envelope);
    }
}
