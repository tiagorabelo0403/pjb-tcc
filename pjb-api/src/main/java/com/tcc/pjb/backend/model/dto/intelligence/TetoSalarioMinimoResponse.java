package com.tcc.pjb.backend.model.dto.intelligence;

import java.math.BigDecimal;

public record TetoSalarioMinimoResponse(
        Long id,
        Integer anoReferencia,
        BigDecimal valorMensal,
        String normaReferencia,
        String fonteOficial,
        Boolean ativo
) {
}
