package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialExperiencePreferenceResponse(
        String resolvedExperienceMode,
        String source,
        boolean teamScoped,
        boolean domainScoped,
        boolean institutionalPolicyApplied,
        Long equipeAtivaId,
        String domainCode,
        String principalKey,
        @Schema(description = "Seletor de experiencia de calculo — configuracao por perfil de usuario (magistrado/advogado/perito)", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> selector,
        @Schema(description = "Contexto de politica de calculo por tribunal — regras especificas de competencia territorial", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> policyContext,
        Instant updatedAt
) {
}

