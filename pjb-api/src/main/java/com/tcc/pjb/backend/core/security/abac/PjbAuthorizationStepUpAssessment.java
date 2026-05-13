package com.tcc.pjb.backend.core.security.abac;

public record PjbAuthorizationStepUpAssessment(
        boolean required,
        boolean satisfied,
        String channel,
        String code,
        String userMessage
) {

    public static PjbAuthorizationStepUpAssessment notRequired(String channel, String code) {
        return new PjbAuthorizationStepUpAssessment(false, true, channel, code, "");
    }

    public static PjbAuthorizationStepUpAssessment satisfied(String channel, String code) {
        return new PjbAuthorizationStepUpAssessment(true, true, channel, code, "");
    }

    public static PjbAuthorizationStepUpAssessment requiredButMissing(String channel,
                                                                      String code,
                                                                      String userMessage) {
        return new PjbAuthorizationStepUpAssessment(true, false, channel, code, userMessage == null ? "" : userMessage.trim());
    }

    public boolean deniedByStepUp() {
        return required && !satisfied;
    }
}
