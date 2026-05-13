package com.tcc.pjb.backend.model.dto.governance;

import java.util.List;

public record BuildGateEvaluationResponse(
        boolean approved,
        boolean securityGateApproved,
        boolean routeGateApproved,
        boolean validationGateApproved,
        boolean controllerCoverageGateApproved,
        boolean envelopeGateApproved,
        boolean qualityMatrixGateApproved,
        int totalOutstandingIssues,
        List<String> outstandingIssues,
        List<String> nextActions
) {
    public BuildGateEvaluationResponse {
        outstandingIssues = outstandingIssues == null ? List.of() : List.copyOf(outstandingIssues);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
    }
}
