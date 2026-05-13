package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceCode;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import java.time.Instant;
import java.util.List;
import java.util.Map;

record ProvidencePlan(
        MagistraturaJudicialProvidenceCode code,
        boolean automatic,
        String stageToken,
        String targetInboxKey,
        String targetQueueCode,
        String targetPanelRoute,
        Instant dueAt,
        String summary,
        List<String> reasons,
        List<String> warnings,
        Map<String, Object> metrics,
        boolean reusedExistingWorkItem,
        WorkItemType workItemType,
        int priority,
        boolean blocking,
        String institutionalOwner,
        List<String> dependencies,
        String expectedReturn,
        String completionEvent,
        String confirmationMode
) {
}
