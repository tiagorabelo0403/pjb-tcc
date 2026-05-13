package com.tcc.pjb.backend.model.dto.magistratura;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MagistraturaJudicialProvidenceResponse(
        MagistraturaJudicialProvidenceCode code,
        String label,
        boolean automatic,
        String operationalStage,
        String targetInboxKey,
        String targetQueueCode,
        String targetPanelRoute,
        Instant dueAt,
        Long assignedUserId,
        String assignedUserName,
        String assignedUserEmail,
        String summary,
        List<Map<String, Object>> participants,
        List<String> reasons,
        List<String> warnings,
        Map<String, Object> metrics
) {
}
