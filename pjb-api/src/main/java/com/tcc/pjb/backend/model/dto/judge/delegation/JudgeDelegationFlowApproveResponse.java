package com.tcc.pjb.backend.model.dto.judge.delegation;

public record JudgeDelegationFlowApproveResponse(
        JudgeDelegationFlowView flow,
        JudgeDelegationIssueResponse issuedToken
) {
}
