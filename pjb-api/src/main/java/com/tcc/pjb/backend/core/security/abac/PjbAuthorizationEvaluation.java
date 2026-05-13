package com.tcc.pjb.backend.core.security.abac;

public record PjbAuthorizationEvaluation(AuthzDecision decision, PjbAuthorizationDecisionTrail trail) {

    public boolean allowed() {
        return decision != null && decision.allowed();
    }
}
