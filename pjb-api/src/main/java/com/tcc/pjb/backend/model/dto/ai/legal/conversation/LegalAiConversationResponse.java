package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record LegalAiConversationResponse(
        String conversationId,
        String selectedVersion,
        String selectedCapability,
        String answer,
        List<String> nextSteps,
        List<Map<String, Object>> virtualTrends,
        Map<String, Object> conversationContext,
        Map<String, Object> safeguards
) {
    @JsonProperty("version")
    public String version() {
        return selectedVersion;
    }

    @JsonProperty("capability")
    public String capability() {
        return selectedCapability;
    }

    @JsonProperty("trace")
    public Map<String, Object> trace() {
        return conversationContext;
    }
}
