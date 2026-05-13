package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalSessionRiskAssessmentResponse(
        String assessmentId,
        Long userId,
        String userName,
        String affiliationId,
        String nominationId,
        String unidadeCodigo,
        String caixaCodigo,
        String deviceId,
        String ipAddress,
        String geographicUf,
        int riskScore,
        String riskLevel,
        boolean requiresStepUp,
        boolean requiresManualApproval,
        boolean blocked,
        List<NationalCommunicationInstitutionalSessionRiskFindingResponse> findings,
        List<String> fundamentos,
        Instant assessedAt
) {
}
