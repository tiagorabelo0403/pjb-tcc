package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationRoutingService {

    public ApiVersion resolveVersion(LegalAiConversationRequest request) {
        String message = request == null || request.message() == null ? "" : request.message().trim();
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("jurisprud") || lower.contains("precedent") || lower.contains("acórd") || lower.contains("hermen") || message.length() > 700) {
            return ApiVersion.V3;
        }
        if (lower.contains("prazo") || lower.contains("rito") || lower.contains("compet") || lower.contains("cabimento") || message.length() > 280) {
            return ApiVersion.V2;
        }
        return ApiVersion.V1;
    }

    public String resolveCapability(LegalAiConversationRequest request, ApiVersion version) {
        String message = request == null || request.message() == null ? "" : request.message().toLowerCase(Locale.ROOT);
        if (message.contains("minuta") || message.contains("petição") || message.contains("peticao") || message.contains("despacho") || message.contains("parecer")) {
            return "LEGAL_CHAT_DRAFT_" + version.name();
        }
        if (message.contains("prazo") || message.contains("cabimento") || message.contains("recurso") || message.contains("compet") || message.contains("sigilo")) {
            return "LEGAL_CHAT_VALIDATION_" + version.name();
        }
        if (message.contains("jurisprud") || message.contains("tema") || message.contains("súmula") || message.contains("sumula") || message.contains("precedente")) {
            return "LEGAL_CHAT_RESEARCH_" + version.name();
        }
        return "LEGAL_CHAT_CONVERSATION_" + version.name();
    }
}
