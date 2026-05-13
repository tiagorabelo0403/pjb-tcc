package com.tcc.pjb.backend.model.entity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import com.tcc.pjb.backend.ai.audit.AuditLogger;
import com.tcc.pjb.backend.ai.audit.TelemetryPublisher;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalHash;

public class ChatMensagemListener {

    @PostPersist
    public void onCreate(ChatMensagem msg) {
        try {
            Map<String, Object> payload = buildPayload(msg, "CREATE");
            AuditLogger.log(actor(msg), "chat_mensagem_create", payload.toString());
            TelemetryPublisher.publish(actor(msg), payload);
        } catch (Exception e) {
            AuditLogger.log("system", "chat_mensagem_create_error", e.getMessage());
        }
    }

    @PostUpdate
    public void onUpdate(ChatMensagem msg) {
        try {
            Map<String, Object> payload = buildPayload(msg, "UPDATE");
            AuditLogger.log(actor(msg), "chat_mensagem_update", payload.toString());
            TelemetryPublisher.publish(actor(msg), payload);
        } catch (Exception e) {
            AuditLogger.log("system", "chat_mensagem_update_error", e.getMessage());
        }
    }

    
    
    

    private String actor(ChatMensagem msg) {
        if (msg.getUsuario() != null && msg.getUsuario().getId() != null) {
            return "usuario:" + msg.getUsuario().getId();
        }
        return "system";
    }

    private Map<String, Object> buildPayload(ChatMensagem msg, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("id", msg.getId());
        String content = safe(msg.getConteudo());
        payload.put("contentLen", content.length());
        payload.put("contentHash", RecursalHash.sha256Hex(content));
        payload.put("usuarioId", msg.getUsuario() != null ? msg.getUsuario().getId() : null);
        payload.put("processoId", msg.getProcesso() != null ? msg.getProcesso().getId() : null);
        payload.put("timestamp", LocalDateTime.now().toString());
        return payload;
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\r\\n\\t]", " ").trim();
    }
}