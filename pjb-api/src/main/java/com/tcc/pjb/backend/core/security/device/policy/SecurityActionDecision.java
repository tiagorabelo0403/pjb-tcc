package com.tcc.pjb.backend.core.security.device.policy;

public record SecurityActionDecision(SecurityAction action, String ruleId) {
    public static SecurityActionDecision of(SecurityAction action, String ruleId) {
        return new SecurityActionDecision(action == null ? SecurityAction.UNKNOWN : action, ruleId);
    }
}
