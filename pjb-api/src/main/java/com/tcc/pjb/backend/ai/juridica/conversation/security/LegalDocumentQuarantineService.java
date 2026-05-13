package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer.LegalConversationSanitizationResult;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalSourceAllowlist.LegalSourceAllowlistDecision;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LegalDocumentQuarantineService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg", "webp");
    private static final List<String> SUSPICIOUS_ATTACHMENT_MARKERS = List.of(
            "ignore",
            "prompt",
            "system",
            "override",
            "jailbreak",
            "instruction"
    );

    public LegalAiConversationDocumentSecuritySnapshot inspect(LegalAiConversationRequest request,
                                                               LegalConversationSanitizationResult sanitization,
                                                               LegalSourceAllowlistDecision allowlistDecision) {
        List<String> attachments = request == null || request.attachments() == null ? List.of() : request.attachments();
        LinkedHashSet<String> allowedAttachments = new LinkedHashSet<>();
        LinkedHashSet<String> quarantinedAttachments = new LinkedHashSet<>();
        List<String> alerts = new ArrayList<>();
        for (String attachment : attachments) {
            if (attachment == null || attachment.isBlank()) {
                continue;
            }
            if (isAttachmentAllowed(attachment) && !isSuspiciousAttachment(attachment)) {
                allowedAttachments.add(attachment);
            } else {
                quarantinedAttachments.add(attachment);
            }
        }
        if (allowlistDecision != null && !allowlistDecision.blockedSources().isEmpty()) {
            alerts.add("Fontes externas fora da allowlist foram isoladas da conversa jurídica.");
        }
        if (sanitization != null && sanitization.snapshot() != null && sanitization.snapshot().promptInjectionDetected()) {
            alerts.add("Marcadores de prompt injection exigem quarentena documental e revisão humana antes de ampliar o contexto.");
        }
        if (!quarantinedAttachments.isEmpty()) {
            alerts.add("Anexos fora da política documental permaneceram em quarentena e não seguem para RAG nem para contexto ampliado.");
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("attachmentCount", attachments.size());
        diagnostics.put("allowedAttachmentCount", allowedAttachments.size());
        diagnostics.put("quarantinedAttachmentCount", quarantinedAttachments.size());
        diagnostics.put("allowlistStatus", allowlistDecision == null ? null : allowlistDecision.status());
        diagnostics.put("promptInjectionDetected", sanitization != null && sanitization.snapshot() != null && sanitization.snapshot().promptInjectionDetected());
        String status = !quarantinedAttachments.isEmpty() || allowlistDecision != null && !allowlistDecision.blockedSources().isEmpty()
                ? sanitization != null && sanitization.snapshot() != null && sanitization.snapshot().promptInjectionDetected()
                    ? "HUMAN_REVIEW_REQUIRED"
                    : "QUARANTINED"
                : "CLEARED";
        return new LegalAiConversationDocumentSecuritySnapshot(
                status,
                allowlistDecision == null ? List.of() : allowlistDecision.allowlistedSources(),
                allowlistDecision == null ? List.of() : allowlistDecision.blockedSources(),
                List.copyOf(allowedAttachments),
                List.copyOf(quarantinedAttachments),
                List.copyOf(alerts),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private boolean isAttachmentAllowed(String attachment) {
        String normalized = attachment.trim().toLowerCase(Locale.ROOT);
        int idx = normalized.lastIndexOf('.');
        if (idx < 0 || idx == normalized.length() - 1) {
            return false;
        }
        String extension = normalized.substring(idx + 1);
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private boolean isSuspiciousAttachment(String attachment) {
        String normalized = attachment.trim().toLowerCase(Locale.ROOT);
        return SUSPICIOUS_ATTACHMENT_MARKERS.stream().anyMatch(normalized::contains);
    }
}
