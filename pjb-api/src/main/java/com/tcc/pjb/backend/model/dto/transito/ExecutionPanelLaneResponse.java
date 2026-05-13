package com.tcc.pjb.backend.model.dto.transito;

import java.util.List;
import java.util.Map;

public record ExecutionPanelLaneResponse(
        String code,
        String status,
        int itemCount,
        String descriptor,
        List<String> highlights,
        Map<String, Object> metadata
) {
    public ExecutionPanelLaneResponse {
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
