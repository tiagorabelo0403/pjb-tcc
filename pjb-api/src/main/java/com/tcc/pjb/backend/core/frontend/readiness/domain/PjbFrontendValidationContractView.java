package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.util.List;

public record PjbFrontendValidationContractView(
        boolean ready,
        boolean validationGateApproved,
        boolean methodArgumentValidationHandled,
        boolean constraintViolationHandled,
        boolean standardized422,
        int outstandingValidationIssues,
        List<String> notes
) {
}
