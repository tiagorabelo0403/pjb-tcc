package com.tcc.pjb.backend.model.dto.processual.calculo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CalculoParcelaLivreRequest(
        @NotBlank @Size(max = 64) String codigo,
        @NotBlank @Size(max = 180) String descricao,
        @DecimalMin("0.00") BigDecimal valor,
        @Size(max = 240) String baseLegal
) {
}
