package com.tcc.pjb.backend.service.processual.calculo;

import java.math.BigDecimal;

public record CalculoJudicialLinha(
        String secao,
        String codigo,
        String titulo,
        BigDecimal base,
        BigDecimal quantidade,
        BigDecimal aliquota,
        BigDecimal valor,
        String formula,
        String explicacaoCidadao,
        String explicacaoTecnica,
        String baseLegal
) {
}
