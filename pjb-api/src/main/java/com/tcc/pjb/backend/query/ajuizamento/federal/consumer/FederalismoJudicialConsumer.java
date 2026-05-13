package com.tcc.pjb.backend.query.ajuizamento.federal.consumer;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoEventoRequest;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;

@Component
@ConditionalOnProperty(prefix = "pjb.kafka", name = "enabled", havingValue = "true")
public class FederalismoJudicialConsumer {

    private static final Logger log = LoggerFactory.getLogger(FederalismoJudicialConsumer.class);

    private final FederalismoJudicialEngine federalismoJudicialEngine;

    public FederalismoJudicialConsumer(FederalismoJudicialEngine federalismoJudicialEngine) {
        this.federalismoJudicialEngine = federalismoJudicialEngine;
    }

    @KafkaListener(containerFactory = "pjbKafkaListenerContainerFactory", topics = FederalismoJudicialEngine.TOPIC_FEDERACAO_EVENTOS, groupId = "grupo-federalismo-judicial")
    public void consumir(@Payload Map<String, Object> payload) {
        try {
            federalismoJudicialEngine.registrarEventoFederado(new FederalismoEventoRequest(
                    stringValue(payload.get("tribunalCodigo")),
                    stringValue(payload.get("topicKafka")) != null ? stringValue(payload.get("topicKafka")) : FederalismoJudicialEngine.TOPIC_FEDERACAO_EVENTOS,
                    stringValue(payload.get("tipoEvento")),
                    stringValue(payload.get("nupn")),
                    stringValue(payload.get("operadorId")),
                    stringValue(payload.get("correlationId")),
                    stringValue(payload.get("idempotencyKey")),
                    longValue(payload.get("schemaVersion")),
                    intValue(payload.get("prioridade")),
                    booleanValue(payload.get("validarAssinatura")),
                    stringValue(payload.get("payload")) != null ? stringValue(payload.get("payload")) : "{}",
                    payload
            ));
        } catch (Exception ex) {
            log.warn("Falha ao consumir evento federativo: {}", ex.getMessage());
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static Boolean booleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value).trim());
    }
}
