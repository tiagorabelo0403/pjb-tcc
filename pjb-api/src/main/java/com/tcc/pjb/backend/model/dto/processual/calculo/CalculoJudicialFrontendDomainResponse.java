package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.util.List;
import java.util.Map;

public record CalculoJudicialFrontendDomainResponse(
        String codigo,
        String slug,
        String aba,
        String titulo,
        String descricao,
        Map<String, String> rotas,
        List<Map<String, Object>> secoes,
        List<Map<String, Object>> campos,
        Map<String, Object> resultado,
        Map<String, Object> ux,
        Map<String, Object> erros,
        Map<String, Object> http,
        Map<String, Object> aiAgents,
        Map<String, Object> officialTables,
        Map<String, Object> payloadInicial,
        Map<String, Object> iaRequestExemplo,
        Map<String, Object> requestExemplo,
        Map<String, Object> responseExemplo,
        Map<String, Object> errorExemplo
) {
}
