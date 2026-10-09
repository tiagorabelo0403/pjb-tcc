package com.tcc.pjb.backend.service.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalarioMinimoReferenciaAnual(
        int ano,
        BigDecimal valor,
        LocalDate vigenteDesde,
        String normaReferencia,
        String fonteOficial
) {
}
