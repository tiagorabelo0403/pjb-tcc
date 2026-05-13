package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalSessionRiskAssessment(
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
        List<InstitutionalSessionRiskFinding> findings,
        List<String> fundamentos,
        Instant assessedAt,
        String hashIntegridade
) {
    public InstitutionalSessionRiskAssessment {
        Objects.requireNonNull(assessmentId);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(assessmentId, userId, affiliationId, nominationId, unidadeCodigo, caixaCodigo, deviceId, ipAddress,
                    geographicUf, riskScore, riskLevel, requiresStepUp, requiresManualApproval, blocked, findings, fundamentos, assessedAt);
        }
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_session_risk_assessment");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
