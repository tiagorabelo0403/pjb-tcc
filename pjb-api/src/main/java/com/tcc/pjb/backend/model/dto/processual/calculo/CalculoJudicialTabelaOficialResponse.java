package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CalculoJudicialTabelaOficialResponse(
        String version,
        String fingerprint,
        String dominioFiltrado,
        Map<String, String> rotas,
        List<CalculoJudicialTabelaOficialItemResponse> tabelas,
        Map<String, Object> politicaAtualizacao,
        Instant geradoEm
) {
}
