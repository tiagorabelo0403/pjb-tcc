package com.tcc.pjb.backend.model.dto.magistratura;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MagistraturaJudicialProvidenceDispatchResponse(
        MagistraturaJudicialProvidenceCode code,
        String status,
        String operationalStage,
        Long workItemId,
        boolean reusedExistingWorkItem,
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
        Map<String, Object> metrics
) {
}
