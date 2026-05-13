package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaNotificationEnvelope;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OficialJusticaNotificationEventPublisher {

    public static final String TOPIC = "evento.oficial.justica.notificacao.v1";
    public static final String GROUP_ID = "grupo-oficial-justica-notification";

    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;
    private final OficialJusticaNotificationDispatchService dispatchService;
    private final boolean kafkaEnabled;

    public OficialJusticaNotificationEventPublisher(ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider,
                                                    OficialJusticaNotificationDispatchService dispatchService,
                                                    @Value("${pjb.kafka.enabled:false}") boolean kafkaEnabled) {
        this.kafkaTemplateProvider = Objects.requireNonNull(kafkaTemplateProvider);
        this.dispatchService = Objects.requireNonNull(dispatchService);
        this.kafkaEnabled = kafkaEnabled;
    }

    public void publish(OficialJusticaNotificationEnvelope envelope) {
        if (envelope == null || envelope.usuarioId() == null) {
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
