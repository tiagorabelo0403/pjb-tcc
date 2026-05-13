package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.dto.calendar.CalendarNotificationEnvelope;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CalendarNotificationEventPublisher {

    public static final String TOPIC = "evento.calendario.notificacao.v1";

    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;
    private final CalendarNotificationDispatchService dispatchService;
    private final boolean kafkaEnabled;

    public CalendarNotificationEventPublisher(ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider,
                                              CalendarNotificationDispatchService dispatchService,
                                              @Value("${pjb.kafka.enabled:false}") boolean kafkaEnabled) {
        this.kafkaTemplateProvider = Objects.requireNonNull(kafkaTemplateProvider);
        this.dispatchService = Objects.requireNonNull(dispatchService);
        this.kafkaEnabled = kafkaEnabled;
    }

    public void publish(CalendarNotificationEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        KafkaTemplate<String, Object> kafkaTemplate = kafkaEnabled ? kafkaTemplateProvider.getIfAvailable() : null;
        if (kafkaTemplate == null) {
            dispatchService.dispatch(envelope);
            return;
        }
        kafkaTemplate.send(TOPIC, String.valueOf(envelope.usuarioId()), envelope);
    }
}
