package com.tcc.pjb.backend.model.dto.teto;

import java.math.BigDecimal;

public record SalarioMinimoUpsertRequest(
        Integer anoReferencia,
        BigDecimal valorMensal,
        String normaReferencia,
        String fonteOficial
) {
}
