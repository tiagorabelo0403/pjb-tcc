package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.math.BigDecimal;

public record CalculoJudicialItemResponse(
        String secao,
        String codigo,
        String titulo,
        BigDecimal base,
        BigDecimal quantidade,
        BigDecimal aliquota,
        BigDecimal valor,
        String formula,
        String explicacaoPerfil,
        String baseLegal
) {
}
