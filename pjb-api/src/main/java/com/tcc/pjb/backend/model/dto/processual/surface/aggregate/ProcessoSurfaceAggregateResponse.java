package com.tcc.pjb.backend.model.dto.processual.surface.aggregate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProcessoSurfaceAggregateResponse(
        ProcessoSurfaceIdentityResponse identity,
        String dominio,
        String estado,
        Map<String, String> labels,
        Map<String, Long> metricas,
        List<String> alertas,
        List<String> proximosPassos,
        List<ProcessoSurfaceValueItemResponse> itens,
        Instant generatedAt
) {
}
