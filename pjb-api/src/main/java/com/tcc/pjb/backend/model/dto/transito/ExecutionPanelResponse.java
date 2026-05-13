package com.tcc.pjb.backend.model.dto.transito;

import java.util.List;
import java.util.Map;

public record ExecutionPanelResponse(
        Long processoId,
        String numeroProcesso,
        String statusAtual,
        String faseAtual,
        boolean executionReady,
        long pendenciasOperacionais,
        long bloqueiosOperacionais,
        ExecutionPanelSummaryResponse summary,
        List<ExecutionPanelLaneResponse> lanes,
        List<ExecutionPanelActionResponse> suggestedActions,
        Map<String, Object> integrity,
        Map<String, Object> frontend
) {
    public ExecutionPanelResponse {
        lanes = lanes == null ? List.of() : List.copyOf(lanes);
        suggestedActions = suggestedActions == null ? List.of() : List.copyOf(suggestedActions);
        integrity = integrity == null ? Map.of() : Map.copyOf(integrity);
        frontend = frontend == null ? Map.of() : Map.copyOf(frontend);
    }
}
