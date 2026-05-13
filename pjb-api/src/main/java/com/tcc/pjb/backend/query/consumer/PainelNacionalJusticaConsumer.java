package com.tcc.pjb.backend.query.consumer;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.painel.TipoEventoPainelNacional;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService;

@Component
@ConditionalOnProperty(prefix = "pjb.kafka", name = "enabled", havingValue = "true")
public class PainelNacionalJusticaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PainelNacionalJusticaConsumer.class);
    private static final String TOPIC = "pjb.nacional.painel.eventos.v1";

    private final PainelNacionalJusticaService painelNacionalJusticaService;

    public PainelNacionalJusticaConsumer(PainelNacionalJusticaService painelNacionalJusticaService) {
        this.painelNacionalJusticaService = painelNacionalJusticaService;
    }

    @KafkaListener(containerFactory = "pjbKafkaListenerContainerFactory", topics = TOPIC, groupId = "grupo-painel-nacional")
    public void consumir(@Payload Map<String, Object> payload) {
        try {
            TipoEventoPainelNacional tipoEvento = enumValue(payload.get("tipoEvento"));
            if (tipoEvento == null || tipoEvento == TipoEventoPainelNacional.AJUIZADO) {
                painelNacionalJusticaService.registrarEventoAjuizado(
                        stringValue(payload.get("tribunalCodigo")),
                        stringValue(payload.get("ramoDireito")),
                        instantValue(payload.get("ocorridoEm"))
                );
            }
        } catch (Exception ex) {
            log.warn("Falha ao consumir evento do painel nacional: {}", ex.getMessage());
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static Instant instantValue(Object value) {
        String s = stringValue(value);
        if (s == null) {
            return Instant.now();
        }
        try {
            return Instant.parse(s);
        } catch (Exception ex) {
            return Instant.now();
        }
    }

    private static TipoEventoPainelNacional enumValue(Object value) {
        String s = stringValue(value);
        if (s == null) {
            return null;
        }
        try {
            return TipoEventoPainelNacional.valueOf(s.trim().toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }
}
