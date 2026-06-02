package com.tcc.pjb.backend.model.dto.magistratura;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record MagistraturaJudicialActCommandResponse(
        MagistraturaJudicialActCode action,
        String lane,
        String status,
        Long processoId,
        List<String> reasons,
        List<MagistraturaJudicialProvidenceDispatchResponse> providences,
        @Schema(description = "Payload do comando judicial — polimórfico por tipo de ato (despacho/sentença/decisão)", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> payload
) {
}

