package com.tcc.pjb.backend.model.dto.leitura;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ProcessReadingNavigationResponse(
        Long processoId,
        String navigationMode,
        String chronologyMode,
        int totalNodes,
        List<ProcessReadingNavigationNodeResponse> nodes,
        @Schema(description = "Metadados tecnicos da navegacao do processo")
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metadata
) {
    public ProcessReadingNavigationResponse {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}

