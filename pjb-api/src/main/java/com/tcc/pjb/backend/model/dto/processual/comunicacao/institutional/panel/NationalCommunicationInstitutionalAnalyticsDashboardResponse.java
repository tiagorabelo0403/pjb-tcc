package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record NationalCommunicationInstitutionalAnalyticsDashboardResponse(
        Map<String, NationalCommunicationInstitutionalAnalyticsBucketResponse> falhasPorMotivo,
        Map<String, NationalCommunicationInstitutionalAnalyticsBucketResponse> redistribuicoesPorAtoCanonico,
        Map<String, NationalCommunicationInstitutionalAnalyticsBucketResponse> minutasPorStatus,
        Map<String, NationalCommunicationInstitutionalAnalyticsBucketResponse> delegacoesPorTipo,
        double mediaHorasCienciaAteCumprimento,
        double mediaHorasCienciaAtePeticao,
        long totalDelegacoesAtivas,
        long totalSubstituicoesAtivas,
        long totalMinutasPendentesAprovacao,
        List<String> insights,
        Instant generatedAt
) {
}
