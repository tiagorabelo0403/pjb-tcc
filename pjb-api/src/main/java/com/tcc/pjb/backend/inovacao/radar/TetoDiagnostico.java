package com.tcc.pjb.backend.inovacao.radar;

import java.math.BigDecimal;
import java.math.RoundingMode;

record TetoDiagnostico(boolean suspeito,
                       com.tcc.pjb.backend.service.exception.enums.TipoViolacaoTeto tipoViolacao,
                       BigDecimal limite,
                       BigDecimal margemAteLimite,
                       int repeticaoEscritorio) {
    BigDecimal violacaoPercentual() {
        if (limite == null || limite.signum() <= 0 || margemAteLimite == null) {
            return BigDecimal.ONE;
        }
        return margemAteLimite.abs().divide(limite, 6, RoundingMode.HALF_UP);
    }
}
