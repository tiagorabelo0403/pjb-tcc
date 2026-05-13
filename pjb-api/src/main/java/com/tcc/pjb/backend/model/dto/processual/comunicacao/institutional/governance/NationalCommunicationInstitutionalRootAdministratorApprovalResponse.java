package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalRootAdministratorApprovalResponse(
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
        Instant updatedAt
) {
}
