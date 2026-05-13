package com.tcc.pjb.backend.query.consumer;

import java.time.Instant;
import com.tcc.pjb.backend.core.lgpd.PjbProcessoSigiloRlsEntryPointSupport;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.identity.ProntuarioNacionalEntrada;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;

@Component
@ConditionalOnProperty(prefix = "pjb.kafka", name = "enabled", havingValue = "true")
public class ProntuarioNacionalConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProntuarioNacionalConsumer.class);

    private final ProntuarioNacionalService prontuarioNacionalService;
    private final PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport;

    public ProntuarioNacionalConsumer(ProntuarioNacionalService prontuarioNacionalService,
                                      PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport) {
        this.prontuarioNacionalService = prontuarioNacionalService;
        this.processoSigiloRlsEntryPointSupport = processoSigiloRlsEntryPointSupport;
    }

    @KafkaListener(containerFactory = "pjbKafkaListenerContainerFactory", topics = ProntuarioNacionalService.TOPIC_PRONTUARIO_NACIONAL, groupId = "grupo-prontuario-nacional")
    public void consumir(@Payload Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        Long processoId = longValue(payload.get("processoLocalId"));
        processoSigiloRlsEntryPointSupport.runWithProcessoContext(processoId, "KAFKA", () -> {
            try {
                prontuarioNacionalService.consumirEvento(new ProntuarioNacionalService.EventoProcessoRegistrado(
                    stringValue(payload.get("eventoId")),
                    stringValue(payload.get("documento")),
                    stringValue(payload.get("nomeSujeito")),
                    stringValue(payload.get("nupn")),
                    longValue(payload.get("processoLocalId")),
                    stringValue(payload.get("tribunalCodigo")),
                    enumValue(ProntuarioNacionalEntrada.PoloProcessual.class, payload.get("polo"), ProntuarioNacionalEntrada.PoloProcessual.INTERESSADO),
                    enumValue(ProntuarioNacionalEntrada.QualificacaoProcessual.class, payload.get("qualificacao"), ProntuarioNacionalEntrada.QualificacaoProcessual.INTERESSADO),
                    enumValue(RamoDireito.class, payload.get("ramoDireito"), null),
                    enumValue(StatusProcesso.class, payload.get("statusProcesso"), StatusProcesso.EM_ANDAMENTO),
                    stringValue(payload.get("classeProcessual")),
                    stringValue(payload.get("assunto")),
                    enumValue(NivelSigilo.class, payload.get("nivelSigilo"), null),
                    stringValue(payload.get("tribunalOrigemUri")),
                    instantValue(payload.get("ocorridoEm")),
                    stringValue(payload.get("topic"))
                ));
            } catch (Exception ex) {
                log.warn("Falha ao consumir evento de prontuario nacional: {}", ex.getMessage());
            }
        });
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

    private static <E extends Enum<E>> E enumValue(Class<E> type, Object raw, E fallback) {
        String s = stringValue(raw);
        if (s == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, s);
        } catch (Exception ex) {
            return fallback;
        }
    }
}
