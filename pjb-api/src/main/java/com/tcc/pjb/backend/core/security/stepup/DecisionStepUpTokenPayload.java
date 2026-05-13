package com.tcc.pjb.backend.core.security.stepup;

public record DecisionStepUpTokenPayload(
        String jti,
        Long userId,
        long iat,
        long exp,
        String level,
        String actType,
        Long processoId,
        Long focusSessionId,
        String windowBinding,
        String tabBinding,
        String routeBinding,
        String requestHash
) {
}
