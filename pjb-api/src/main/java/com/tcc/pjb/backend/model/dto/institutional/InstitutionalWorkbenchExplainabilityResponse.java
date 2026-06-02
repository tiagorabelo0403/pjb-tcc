package com.tcc.pjb.backend.model.dto.institutional;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record InstitutionalWorkbenchExplainabilityResponse(
        String actorBranch,
        String targetSphere,
        String verdict,
        List<String> reasons,
        List<String> warnings,
        @Schema(description = "Metricas de explainabilidade institucional — chaves numéricas por domínio de análise", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metrics
) {
}

