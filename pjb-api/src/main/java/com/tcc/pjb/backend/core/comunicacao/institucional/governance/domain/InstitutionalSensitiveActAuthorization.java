package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalSensitiveActAuthorization(
        String authorizationId,
        InstitutionalSensitiveAct sensitiveAct,
        Long userId,
        String userName,
        String affiliationId,
        String nominationId,
        InstitutionalTrustLevel achievedTrust,
        InstitutionalTrustLevel requiredTrust,
        boolean allowed,
        boolean requiresManualApproval,
        boolean blocked,
        List<String> findings,
        List<String> fundamentos,
        Instant evaluatedAt,
        String hashIntegridade
) {
    public InstitutionalSensitiveActAuthorization {
        Objects.requireNonNull(authorizationId);
        Objects.requireNonNull(sensitiveAct);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(authorizationId, sensitiveAct, userId, affiliationId, nominationId, achievedTrust, requiredTrust,
                    allowed, requiresManualApproval, blocked, findings, fundamentos, evaluatedAt);
        }
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_sensitive_act_authorization");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
