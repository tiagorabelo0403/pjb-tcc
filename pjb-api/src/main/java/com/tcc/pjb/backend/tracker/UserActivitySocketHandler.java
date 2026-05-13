package com.tcc.pjb.backend.tracker;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "pjb.kafka", name = "enabled", havingValue = "true")
public class UserActivitySocketHandler extends TextWebSocketHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserActivitySocketHandler.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "evento.usuario.atividade.v1";

    public UserActivitySocketHandler(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String usuarioId = extractUsuarioId(session);
        if (usuarioId == null || usuarioId.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put("usuarioId", usuarioId);
        log.info("WebSocket iniciado para usuário {} (sessão {})", usuarioId, session.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String usuarioId = (String) session.getAttributes().get("usuarioId");
        if (usuarioId == null || usuarioId.isBlank()) return;

        Map<String, Object> data;
        try {
            data = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (Exception ex) {
            data = new HashMap<>();
            data.put("raw", message.getPayload());
            data.put("parseError", ex.getClass().getSimpleName());
        }

        data.put("usuarioId", usuarioId);
        data.put("timestamp", System.currentTimeMillis());
        data.put("sessaoId", session.getId());

        kafkaTemplate.send(TOPIC, usuarioId, data);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket encerrado (sessão {}): {}", session.getId(), status);
    }

    private String extractUsuarioId(WebSocketSession session) {
        
        if (session.getPrincipal() != null && session.getPrincipal().getName() != null) {
            String n = session.getPrincipal().getName().trim();
            if (!n.isBlank()) return n;
        }

        
        try {
            if (session.getUri() != null && session.getUri().getQuery() != null) {
                Map<String, String> q = parseQuery(session.getUri().getQuery());
                String u = q.getOrDefault("usuarioId", q.get("userId"));
                if (u != null && !u.isBlank()) return u.trim();
            }
        } catch (Exception ex) {
            log.debug("falha ao extrair usuarioId da query da sessão {}", session.getId(), ex);
        }


        
        Object v = session.getAttributes().get("usuarioId");
        return v != null ? String.valueOf(v).trim() : null;
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null || raw.isBlank()) return out;
        for (String kv : raw.split("&")) {
            if (kv.isBlank()) continue;
            int idx = kv.indexOf('=');
            String k = idx >= 0 ? kv.substring(0, idx) : kv;
            String v = idx >= 0 ? kv.substring(idx + 1) : "";
            k = URLDecoder.decode(k, StandardCharsets.UTF_8);
            v = URLDecoder.decode(v, StandardCharsets.UTF_8);
            if (!k.isBlank()) out.put(k, v);
        }
        return out;
    }
}
