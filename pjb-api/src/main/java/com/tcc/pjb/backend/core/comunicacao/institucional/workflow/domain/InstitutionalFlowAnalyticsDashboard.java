package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InstitutionalFlowAnalyticsDashboard(
        Map<String, InstitutionalFlowAnalyticsBucket> falhasPorMotivo,
        Map<String, InstitutionalFlowAnalyticsBucket> redistribuicoesPorAtoCanonico,
        Map<String, InstitutionalFlowAnalyticsBucket> minutasPorStatus,
        Map<String, InstitutionalFlowAnalyticsBucket> delegacoesPorTipo,
        double mediaHorasCienciaAteCumprimento,
        double mediaHorasCienciaAtePeticao,
        long totalDelegacoesAtivas,
        long totalSubstituicoesAtivas,
        long totalMinutasPendentesAprovacao,
        List<String> insights,
        Instant generatedAt
) {
}
