package com.tcc.pjb.backend.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.outbox.OutboxGenericDispatchedEvent;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IntimacaoNotificacaoOutboxListener {

    private static final String EVENT_TYPE = "INTIMACAO_PROCESSUAL_CRIADA";

    private final UsuarioRepository usuarioRepository;
    private final ProcessoRepository processoRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public IntimacaoNotificacaoOutboxListener(UsuarioRepository usuarioRepository,
                                              ProcessoRepository processoRepository,
                                              NotificationService notificationService,
                                              ObjectMapper objectMapper) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @EventListener
    @Transactional
    public void onGenericDispatched(OutboxGenericDispatchedEvent event) {
        if (event == null || !EVENT_TYPE.equals(event.eventType())) {
            return;
        }
        Map<String, Object> payload = parse(event.payloadJson());
        Long usuarioId = longValue(payload.get("usuarioId"));
        Long processoId = longValue(payload.get("processoId"));
        if (usuarioId == null || processoId == null) {
            return;
        }
        Usuario destinatario = usuarioRepository.findById(usuarioId).orElse(null);
        Processo processo = processoRepository.findById(processoId).orElse(null);
        if (destinatario == null || processo == null) {
            return;
        }
        String numeroProcesso = stringValue(payload.get("numeroProcesso"));
        String referencia = numeroProcesso == null ? String.valueOf(processoId) : numeroProcesso;
        notificationService.notifyUser(
                destinatario,
                processo,
                "Nova intimação no processo " + referencia,
                "Um despacho foi publicado no seu processo. O prazo passa a correr a partir da publicação no Diário de Justiça Eletrônico.",
                null);
    }

    private Map<String, Object> parse(String payloadJson) {
        try {
            if (payloadJson == null || payloadJson.isBlank()) {
                return Map.of();
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> values = objectMapper.readValue(payloadJson, Map.class);
            return values == null ? Map.of() : values;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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
}
