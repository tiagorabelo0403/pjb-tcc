package com.tcc.pjb.backend.core.security.abac;

public record PjbAuthorizationGovernanceAssessment(
        boolean required,
        boolean satisfied,
        String channel,
        String code,
        String scope,
        String userMessage
) {

    public static PjbAuthorizationGovernanceAssessment notRequired(String channel,
                                                                   String code,
                                                                   String scope) {
        return new PjbAuthorizationGovernanceAssessment(false, true, channel, code, scope, "");
    }

    public static PjbAuthorizationGovernanceAssessment satisfied(String channel,
                                                                 String code,
                                                                 String scope) {
        return new PjbAuthorizationGovernanceAssessment(true, true, channel, code, scope, "");
    }

    public static PjbAuthorizationGovernanceAssessment requiredButMissing(String channel,
                                                                          String code,
                                                                          String scope,
                                                                          String userMessage) {
        return new PjbAuthorizationGovernanceAssessment(
                true,
                false,
                channel,
                code,
                scope,
                userMessage == null ? "" : userMessage.trim()
        );
    }

    public boolean deniedByGovernance() {
        return required && !satisfied;
    }
}
