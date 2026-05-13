package com.tcc.pjb.backend.model.dto.distribuicao;

import java.util.List;
import java.util.Map;

public record DistributionWorkbenchLaneResponse(
        String code,
        String status,
        String descriptor,
        List<String> highlights,
        Map<String, Object> metadata
) {
    public DistributionWorkbenchLaneResponse {
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
