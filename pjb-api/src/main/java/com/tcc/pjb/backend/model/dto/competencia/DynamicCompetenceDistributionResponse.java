package com.tcc.pjb.backend.model.dto.competencia;

import java.time.Instant;
import java.util.List;

public record DynamicCompetenceDistributionResponse(
        String requestId,
        Instant generatedAt,
        String nupn,
        String unidadeCodigo,
        String tribunalCodigo,
        String comarca,
        String uf,
        String tipoVara,
        double scoreFinal,
        boolean distribuicaoAutomatica,
        String motivacao,
        List<String> alertas,
        List<String> fatoresRevisaoHumana,
        List<AlternativeUnit> alternativas,
        ScoreBreakdown scoreBreakdown
) {

    public record AlternativeUnit(
            String unidadeCodigo,
            String tribunalCodigo,
            String comarca,
            String uf,
            String tipoVara,
            double scoreFinal
    ) {
    }

    public record ScoreBreakdown(
            double territorial,
            double especialidade,
            double disponibilidade,
            double equilibrio,
            double aderenciaNormativa
    ) {
    }
}
