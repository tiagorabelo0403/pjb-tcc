package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalAffiliationApprovalTrail(
        String trailId,
        String requestId,
        Long representativeUserId,
        String representativeName,
        boolean representativeSigned,
        Instant representativeSignedAt,
        Long pjbApproverUserId,
        String pjbApproverName,
        Boolean approvedByPjb,
        Instant pjbDecidedAt,
        boolean dualKeySatisfied,
        String currentStatus,
        List<String> fundamentos,
        Instant updatedAt,
        String hashIntegridade
) {
    public InstitutionalAffiliationApprovalTrail {
        Objects.requireNonNull(trailId);
        Objects.requireNonNull(requestId);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(trailId, requestId, representativeUserId, representativeSigned, representativeSignedAt,
                    pjbApproverUserId, approvedByPjb, pjbDecidedAt, dualKeySatisfied, currentStatus, fundamentos, updatedAt);
        }
    }

    public InstitutionalAffiliationApprovalTrail withDecision(Long approverUserId,
                                                              String approverName,
                                                              boolean approved,
                                                              String status,
                                                              List<String> extraFundamentos,
                                                              Instant now) {
        return new InstitutionalAffiliationApprovalTrail(
                trailId,
                requestId,
                representativeUserId,
                representativeName,
                representativeSigned,
                representativeSignedAt,
                approverUserId,
                approverName,
                approved,
                now,
                representativeSigned && approverUserId != null,
                status,
                merge(extraFundamentos),
                now,
                null
        );
    }

    private List<String> merge(List<String> extras) {
        if (extras == null || extras.isEmpty()) {
            return fundamentos;
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<>(fundamentos);
        out.addAll(extras);
        return List.copyOf(out.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList());
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_affiliation_approval_trail");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
