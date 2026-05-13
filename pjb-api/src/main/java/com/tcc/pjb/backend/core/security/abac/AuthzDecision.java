package com.tcc.pjb.backend.core.security.abac;


public record AuthzDecision(boolean allowed, String reason, String policyVersion) {

    public static AuthzDecision allow(String reason, String policyVersion) {
        return new AuthzDecision(true, reason, policyVersion);
    }

    public static AuthzDecision deny(String reason, String policyVersion) {
        return new AuthzDecision(false, reason, policyVersion);
    }
}
