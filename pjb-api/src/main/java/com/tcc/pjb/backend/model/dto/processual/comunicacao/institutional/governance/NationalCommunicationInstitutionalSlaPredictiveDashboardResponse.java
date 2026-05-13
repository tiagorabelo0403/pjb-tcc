package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalSlaPredictiveDashboardResponse(
        List<NationalCommunicationInstitutionalSlaPredictiveAlertResponse> alertas,
        long totalUnidadesMonitoradas,
        long totalCriticos,
        long totalAltos,
        long totalMedios,
        long totalBaixos,
        Instant generatedAt
) {
}
