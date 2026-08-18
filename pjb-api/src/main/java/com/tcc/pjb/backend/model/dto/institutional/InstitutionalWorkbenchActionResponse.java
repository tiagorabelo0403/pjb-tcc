package com.tcc.pjb.backend.model.dto.institutional;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record InstitutionalWorkbenchActionResponse(
        String code,
        String label,
        String route,
        String method,
        boolean enabled,
        String verdict,
        String severity,
        String redirectRoute,
        List<String> reasons,
        List<String> warnings,
        @Schema(description = "Metricas de acao do workbench institucional — chaves numericas variam por tipo de acao", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metrics
) {
}

