package com.tcc.pjb.backend.model.dto.leilao;

import java.math.BigDecimal;
import java.time.Instant;

public record LeilaoAnaliseLanceHistoricoItem(
        BigDecimal valor,
        String documentoLicitante,
        String ipOrigem,
        Instant ofertadoEm
) {
}
