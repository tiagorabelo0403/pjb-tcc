package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustApprovalKind;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalTrustApprovalDecision(
        String decisionId,
        String profileKey,
        String affiliationId,
        String nominationId,
        Long nominatedUserId,
        InstitutionalTrustApprovalKind approvalKind,
        Long approverUserId,
        String approverUserName,
        boolean approved,
        List<String> fundamentos,
        Instant decidedAt,
        String hashIntegridade
) {
    public InstitutionalTrustApprovalDecision {
        Objects.requireNonNull(decisionId);
        Objects.requireNonNull(profileKey);
        Objects.requireNonNull(approvalKind);
        Objects.requireNonNull(approverUserId);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(decisionId, profileKey, affiliationId, nominationId, nominatedUserId, approvalKind, approverUserId, approverUserName, approved, fundamentos, decidedAt);
        }
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_trust_approval_decision");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
