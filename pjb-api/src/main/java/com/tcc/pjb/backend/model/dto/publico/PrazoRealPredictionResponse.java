package com.tcc.pjb.backend.model.dto.publico;

import java.time.LocalDate;
import java.util.List;

public record PrazoRealPredictionResponse(
        Long processoId,
        String processoNumero,
        String tipoAto,
        long prazoNominalDias,
        long prazoRealEstimadoDias,
        double desvioPercentual,
        LocalDate dataPrevistaConclusao,
        double congestionScore,
        long backlogTerritorial,
        long referenciasHistoricas,
        String modelVersion,
        double riskProbability,
        String riskLevel,
        String uiBand,
        double workloadScore,
        double userPressureScore,
        double complexityScore,
        long openItems,
        long dueSoonItems,
        long overdueItems,
        List<String> fundamentos
) {
    public LocalDate dataEstimada() {
        return dataPrevistaConclusao();
    }
}
