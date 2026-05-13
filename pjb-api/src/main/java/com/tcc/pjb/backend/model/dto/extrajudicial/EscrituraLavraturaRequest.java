package com.tcc.pjb.backend.model.dto.extrajudicial;

import java.math.BigDecimal;

public record EscrituraLavraturaRequest(
        String tipo,
        String atoResumo,
        String partesResumo,
        String bensResumo,
        BigDecimal valorDeclarado
) {
}
