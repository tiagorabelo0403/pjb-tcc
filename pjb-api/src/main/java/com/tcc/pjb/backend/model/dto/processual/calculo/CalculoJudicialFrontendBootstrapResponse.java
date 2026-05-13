package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.Map;

public record CalculoJudicialFrontendBootstrapResponse(
        String codigo,
        String slug,
        CalculoJudicialSolicitantePerfil perfilResolvido,
        Map<String, String> rotas,
        Map<String, Object> http,
        Map<String, Object> aiAgents,
        Map<String, Object> officialTables,
        Map<String, Object> payloadInicial,
        Map<String, Object> iaRequestExemplo,
        Map<String, Object> requestExemplo,
        Map<String, Object> responseExemplo,
        Map<String, Object> errorExemplo,
        Instant geradoEm
) {
}
