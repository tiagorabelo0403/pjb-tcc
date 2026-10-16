package com.tcc.pjb.backend.model.dto.competencia;

import java.util.List;

public record DynamicCompetenceRedistributionResponse(
        List<Proposal> propostas,
        List<Saturacao> saturacoes
) {
    public record Proposal(
            String origemCodigo,
            String destinoCodigo,
            int quantidadeSugerida,
            String justificativa,
            double scoreCompatibilidade
    ) {
    }

    public record Saturacao(
            String unidadeCodigo,
            String tribunalSigla,
            String comarca,
            String uf,
            String tipoVara,
            long cargaRecentePonderada,
            double mediaDosPares,
            String nivelDosPares,
            int quantidadeDePares,
            double razaoSobreOsPares
    ) {
    }
}
