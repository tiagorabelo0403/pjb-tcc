package com.tcc.pjb.backend.model.dto.magistratura;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record MagistraturaJudicialActPreviewResponse(
        Long processoId,
        String processoNumero,
        MagistraturaJudicialActCode action,
        boolean allowed,
        String verdict,
        String lane,
        String suggestedTitle,
        String nativeRoute,
        String template,
        List<String> reasons,
        List<String> warnings,
        List<MagistraturaJudicialActFieldResponse> fields,
        List<MagistraturaJudicialProvidenceResponse> providences,
        @Schema(description = "Metricas de preview do ato judicial — chaves numericas por tipo de ato", implementation = Object.class)
        @Size(max = 20)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> metrics
) {
}

