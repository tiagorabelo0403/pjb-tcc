package com.tcc.pjb.backend.model.dto.magistratura;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

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
        @Schema(description = "Participantes da providencia — estrutura varia por papel processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<Map<String, Object>> participants,
        List<String> reasons,
        @Schema(description = "Metricas de despacho de providencia judicial — chaves variam por tipo de providencia", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metrics
) {
}

