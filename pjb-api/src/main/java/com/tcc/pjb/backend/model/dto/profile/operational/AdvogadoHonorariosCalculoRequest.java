package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AdvogadoHonorariosCalculoRequest(
        @NotNull @Positive BigDecimal valorCondenacao,
        boolean fazendaPublicaVencida,
        boolean causaSimples,
        boolean trabalhoComplexo,
        @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1.0") BigDecimal percentualFixadoMagistrado
) {}
