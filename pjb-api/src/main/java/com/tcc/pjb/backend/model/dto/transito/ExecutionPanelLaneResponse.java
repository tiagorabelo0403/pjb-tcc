package com.tcc.pjb.backend.model.dto.transito;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ExecutionPanelLaneResponse(
        String code,
        String status,
        int itemCount,
        String descriptor,
        List<String> highlights,
        @Schema(description = "Metadados da lane de execucao — chaves variam por tipo de lane judicial", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public ExecutionPanelLaneResponse {
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

