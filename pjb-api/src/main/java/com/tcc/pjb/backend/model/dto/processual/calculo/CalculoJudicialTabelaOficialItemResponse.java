package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.util.List;
import java.util.Map;

public record CalculoJudicialTabelaOficialItemResponse(
        String codigo,
        String dominio,
        String titulo,
        String orgaoOficial,
        String urlOficial,
        String modoAdocaoPjb,
        String referenciaTemporal,
        String vigenciaPjbInicio,
        String vigenciaPjbFim,
        String fingerprint,
        String algoritmoFingerprint,
        Map<String, Object> cobertura,
        Map<String, Object> interoperabilidade,
        Map<String, Object> diffAtual,
        List<Map<String, Object>> trilhaAtualizacao
) {
}
