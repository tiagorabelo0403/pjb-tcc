package com.tcc.pjb.backend.model.dto.distribuicao;

import java.util.List;
import java.util.Map;

public record DistributionWorkbenchResponse(
        String numeroProcesso,
        boolean encontrado,
        DistributionWorkbenchSummaryResponse summary,
        List<DistributionWorkbenchLaneResponse> lanes,
        List<DistributionWorkbenchActionResponse> suggestedActions,
        Map<String, Object> integrity,
        Map<String, Object> frontend
) {
    public DistributionWorkbenchResponse {
        lanes = lanes == null ? List.of() : List.copyOf(lanes);
        suggestedActions = suggestedActions == null ? List.of() : List.copyOf(suggestedActions);
        integrity = integrity == null ? Map.of() : Map.copyOf(integrity);
        frontend = frontend == null ? Map.of() : Map.copyOf(frontend);
    }
}
