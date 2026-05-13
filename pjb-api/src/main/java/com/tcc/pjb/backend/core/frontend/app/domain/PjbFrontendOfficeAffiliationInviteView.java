package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.Instant;
import java.util.List;

public record PjbFrontendOfficeAffiliationInviteView(
        Long inviteId,
        Long equipeId,
        String equipeNome,
        Long patronoUserId,
        String patronoNome,
        String status,
        String invitedNome,
        String invitedEmailMasked,
        String invitedCpfMasked,
        String invitedOab,
        String papelEquipe,
        String cargo,
        List<String> allowedRamos,
        boolean canViewAllRamos,
        Integer minTrustForAuto,
        Integer maxAutoPorDia,
        boolean blockPersonalCases,
        boolean autoActivateOnAccept,
        String modeOnAccept,
        Integer workspacePriority,
        boolean reinforcedFlow,
        String requiredGovBrAssuranceLevel,
        boolean awaitingFinalApproval,
        boolean manageableByCurrentUser,
        boolean actionableByCurrentUser,
        Instant createdAt,
        Instant acceptedAt,
        Instant expiresAt,
        Instant finalApprovalDeadlineAt
) {
}
