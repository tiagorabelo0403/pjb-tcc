package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record NegotiationChatDigestReport(
        String scope,
        String status,
        double confidence,
        String conversationStage,
        String posture,
        String counterpartyTemperature,
        String sendMode,
        String suggestedNextMessage,
        List<String> anchorNarratives,
        List<String> protectedTopics,
        List<String> escalationSignals,
        List<String> nextTurnObjectives,
        List<String> forbiddenMoves,
        List<String> internalActions,
        List<String> messageBlueprints,
        Map<String, Object> diagnostics
) {
    public String temperature() {
        return counterpartyTemperature;
    }
}
