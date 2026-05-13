package com.tcc.pjb.backend.model.dto.ministro;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RepercussaoGeralReconhecimentoRequest(
        @NotBlank String modalidade,
        @NotBlank String ementa,
        String fundamentosResumo,
        @Min(1) @Max(500) Integer limitProcessosRelacionados,
        @Min(55) @Max(99) Integer corteMinimoSimilaridadePercent
) {
}
