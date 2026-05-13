package com.tcc.pjb.backend.model.dto.admin;



import java.time.OffsetDateTime;
import java.util.UUID;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoRuleProposalStatus;

public record RitoRuleProposalDto(
        UUID id,
        String ritoResolved,
        String ritoChosen,
        Integer occurrences,
        String sampleReasonsJson,
        boolean requiresDualApproval,
        RitoRuleProposalStatus status,
        String notes,
        OffsetDateTime createdAt,
        Long createdByUserId,
        OffsetDateTime firstReviewedAt,
        Long firstReviewedByUserId,
        String firstDecisionNotes,
        OffsetDateTime reviewedAt,
        Long reviewedByUserId,
        String secondDecisionNotes,
        String decisionNotes
) {
}
