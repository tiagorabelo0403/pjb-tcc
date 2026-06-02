package com.tcc.pjb.backend.model.dto.leitura;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tcc.pjb.backend.model.dto.shared.reading.ProcessReadingWorkspaceIntegrityDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record ProcessReadingWorkspaceResponse(
        Long processoId,
        String numeroProcesso,
        ProcessReadingSummaryResponse summary,
        ProcessReadingPreferenceResponse preference,
        ProcessReadingNavigationResponse navigation,
        ProcessReadingFlowResponse processFlow,
        ProcessReadingProceduralContextResponse proceduralContext,
        ProcessReadingSpecializationResponse specialization,
        List<ProcessReadingDocumentResponse> documents,
        List<ProcessReadingLaneResponse> lanes,
        List<ProcessReadingActionResponse> suggestedActions,
        List<String> alerts,
        ProcessReadingWorkspaceIntegrityDto integrity,
        @Schema(description = "Configuração completa de frontend do workspace — contém endpoints, configurações de leitura e estado agregado do ecossistema")
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> frontend
) {
    public ProcessReadingWorkspaceResponse {
        processFlow = processFlow == null ? new ProcessReadingFlowResponse(0L, 0L, 0L, 0L, null, null, List.of(), null) : processFlow;
        documents = documents == null ? List.of() : List.copyOf(documents);
        lanes = lanes == null ? List.of() : List.copyOf(lanes);
        suggestedActions = suggestedActions == null ? List.of() : List.copyOf(suggestedActions);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }
}
