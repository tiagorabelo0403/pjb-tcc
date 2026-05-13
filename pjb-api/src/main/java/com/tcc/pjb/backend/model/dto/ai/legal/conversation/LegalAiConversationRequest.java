package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.List;
import java.util.Map;

public record LegalAiConversationRequest(
        String conversationId,
        String processoId,
        String message,
        String userProfile,
        List<String> history,
        List<String> attachments,
        Map<String, Object> context
) {
}
