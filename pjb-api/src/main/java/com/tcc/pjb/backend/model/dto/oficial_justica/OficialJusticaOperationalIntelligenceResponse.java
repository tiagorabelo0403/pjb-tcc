package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.util.List;
import java.util.Map;

public record OficialJusticaOperationalIntelligenceResponse(
        String territorio,
        int totalMandadosPendentes,
        int totalMandadosVencendo,
        int totalMandadosAtrasados,
        int totalCitacoes,
        int totalIntimacoes,
        int totalPenhoras,
        List<String> filasCriticas,
        List<DiligenciaView> proximasDiligencias,
        Map<String, Object> metricas
) {
    public OficialJusticaOperationalIntelligenceResponse {
        filasCriticas = filasCriticas == null ? List.of() : List.copyOf(filasCriticas);
        proximasDiligencias = proximasDiligencias == null ? List.of() : List.copyOf(proximasDiligencias);
        metricas = metricas == null ? Map.of() : Map.copyOf(metricas);
    }

    public record DiligenciaView(
            Long workItemId,
            String processoNumero,
            String titulo,
            String comarca,
            String prazo,
            String tipo
    ) {
    }
}
