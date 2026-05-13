package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CalculoJudicialAjuizamentoSignalResponse(
        String status,
        boolean requerCalculo,
        String dominioSugerido,
        List<Map<String, Object>> mensagensTemporarias,
        List<String> recomendacoes,
        List<String> bloqueios,
        Map<String, Object> routes,
        Map<String, Object> economicReferences,
        Map<String, Object> agentResults,
        Map<String, Object> metadata,
        Instant geradoEm
) {
}
