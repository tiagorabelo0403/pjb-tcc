package com.tcc.pjb.backend.model.dto.julgamento.coverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JulgamentoCoverageRequest(
        @NotBlank String actType,
        @Size(max = 20) String recursalSpecies,
        Boolean acordaoColegiado,
        Boolean decisaoMonocratica,
        boolean persistAudit
) {
}
