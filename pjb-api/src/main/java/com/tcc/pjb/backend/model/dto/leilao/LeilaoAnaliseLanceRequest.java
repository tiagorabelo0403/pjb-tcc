package com.tcc.pjb.backend.model.dto.leilao;

import java.math.BigDecimal;
import java.util.List;

public record LeilaoAnaliseLanceRequest(
        String editalId,
        BigDecimal valorLance,
        BigDecimal lanceMinimo,
        BigDecimal incrementoMinimo,
        String documentoLicitante,
        String ipOrigem,
        boolean licitanteHabilitado,
        List<LeilaoAnaliseLanceHistoricoItem> historicoLances
) {
}
