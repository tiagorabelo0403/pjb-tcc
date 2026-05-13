package com.tcc.pjb.backend.model.dto.julgamento.safety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DecisionStepUpIssueRequest(
        @NotBlank String actType,
        @NotNull Long processoId,
        @NotNull Long focusSessionId,
        @NotBlank String windowBinding,
        @NotBlank String tabBinding,
        String routeBinding,
        @NotBlank String requestHash
) {
}
