package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record InstitutionalRootAdministratorApproval(
        String approvalId,
        String affiliationId,
        Long candidateUserId,
        String candidateUserName,
        Long institutionActorUserId,
        String institutionActorName,
        boolean institutionApproved,
        Instant institutionApprovedAt,
        Long pjbActorUserId,
        String pjbActorName,
        boolean pjbApproved,
        Instant pjbApprovedAt,
        boolean requiresDualApproval,
        boolean approved,
        boolean rejected,
        List<String> findings,
        List<String> fundamentos,
        Instant createdAt,
        Instant updatedAt,
        String hashIntegridade
) {
    public InstitutionalRootAdministratorApproval {
        Objects.requireNonNull(approvalId);
        Objects.requireNonNull(affiliationId);
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(approvalId, affiliationId, candidateUserId, institutionApproved, pjbApproved, requiresDualApproval, approved, rejected, findings, fundamentos);
        }
    }

    public InstitutionalRootAdministratorApproval decidir(String source,
                                                          Long actorUserId,
                                                          String actorName,
                                                          boolean decision,
                                                          List<String> extraFundamentos,
                                                          Instant when) {
        String normalized = source == null ? "PJB" : source.trim().toUpperCase(Locale.ROOT);
        Instant ref = when == null ? Instant.now() : when;
        boolean institution = institutionApproved;
        Instant institutionAt = institutionApprovedAt;
        Long institutionActor = institutionActorUserId;
        String institutionName = institutionActorName;
        boolean pjb = pjbApproved;
        Instant pjbAt = pjbApprovedAt;
        Long pjbActor = pjbActorUserId;
        String pjbName = pjbActorName;
        ArrayList<String> mergedFindings = new ArrayList<>(findings);
        if ("INSTITUICAO".equals(normalized) || "INSTITUCIONAL".equals(normalized)) {
            institution = decision;
            institutionAt = ref;
            institutionActor = actorUserId;
            institutionName = actorName;
            mergedFindings.remove("aprovacao_institucional_pendente");
        } else {
            pjb = decision;
            pjbAt = ref;
            pjbActor = actorUserId;
            pjbName = actorName;
            mergedFindings.remove("aprovacao_pjb_pendente");
        }
        boolean rejectedNow = !decision;
        boolean approvedNow = !rejectedNow && (requiresDualApproval ? institution && pjb : institution || pjb);
        if (!approvedNow && !rejectedNow) {
            if (!institution) mergedFindings.add("aprovacao_institucional_pendente");
            if (requiresDualApproval && !pjb) mergedFindings.add("aprovacao_pjb_pendente");
        }
        return new InstitutionalRootAdministratorApproval(
                approvalId,
                affiliationId,
                candidateUserId,
                candidateUserName,
                institutionActor,
                institutionName,
                institution,
                institutionAt,
                pjbActor,
                pjbName,
                pjb,
                pjbAt,
                requiresDualApproval,
                approvedNow,
                rejectedNow,
                mergedFindings.stream().distinct().toList(),
                merge(extraFundamentos),
                createdAt,
                ref,
                null
        );
    }

    public boolean pendente() {
        return !approved && !rejected;
    }

    private List<String> merge(List<String> extras) {
        if (extras == null || extras.isEmpty()) {
            return fundamentos;
        }
        ArrayList<String> out = new ArrayList<>(fundamentos);
        out.addAll(extras);
        return List.copyOf(out);
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_root_admin_approval");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
