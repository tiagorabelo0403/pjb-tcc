package com.tcc.pjb.backend.model.dto.competencia;

import java.util.List;

public record DynamicCompetenceRedistributionResponse(
        List<Proposal> propostas
) {
    public record Proposal(
            String origemCodigo,
            String destinoCodigo,
            int quantidadeSugerida,
            String justificativa,
            double scoreCompatibilidade
    ) {
    }
}
