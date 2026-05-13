package com.tcc.pjb.backend.model.dto.processual.calculo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CalculoIndiceMensalRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String competencia,
        @DecimalMin("0.000000") BigDecimal taxaPercentualMensal
) {
}
