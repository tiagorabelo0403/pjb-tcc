package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;

public record DiligenceRouteOptimizationResponse(
        String actor,
        OriginSnapshot origem,
        double distanciaTotalKm,
        long duracaoEstimadaMinutos,
        Instant chegadaFinalEstimada,
        List<String> warnings,
        List<OptimizedStop> rota,
        List<DeferredStop> adiadas,
        Instant geradoEm
) {

    public record OriginSnapshot(
            String fonte,
            double latitude,
            double longitude,
            Instant capturadoEm,
            Double precisaoMetros
    ) {
    }

    public record OptimizedStop(
            int ordem,
            String id,
            String titulo,
            String endereco,
            double latitude,
            double longitude,
            int prioridade,
            double distanciaTrechoKm,
            long deslocamentoMinutos,
            Instant chegadaEstimada,
            String classificacao
    ) {
    }

    public record DeferredStop(
            String id,
            String titulo,
            String motivo
    ) {
    }
}
