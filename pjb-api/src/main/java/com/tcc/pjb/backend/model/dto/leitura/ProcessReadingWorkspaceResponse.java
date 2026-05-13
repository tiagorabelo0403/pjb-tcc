package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
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
        Map<String, Object> integrity,
        Map<String, Object> frontend
) {
    public ProcessReadingWorkspaceResponse {
        processFlow = processFlow == null ? new ProcessReadingFlowResponse(0L, 0L, 0L, 0L, null, null, List.of(), Map.of()) : processFlow;
        documents = documents == null ? List.of() : List.copyOf(documents);
        lanes = lanes == null ? List.of() : List.copyOf(lanes);
        suggestedActions = suggestedActions == null ? List.of() : List.copyOf(suggestedActions);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
        integrity = integrity == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(integrity));
        frontend = frontend == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(frontend));
    }
}
