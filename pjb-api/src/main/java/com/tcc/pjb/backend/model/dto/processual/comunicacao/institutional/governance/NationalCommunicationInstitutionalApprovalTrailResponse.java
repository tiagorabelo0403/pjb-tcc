package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalApprovalTrailResponse(
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
        Instant updatedAt
) {
}
