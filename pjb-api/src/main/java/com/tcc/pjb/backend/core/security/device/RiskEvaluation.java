package com.tcc.pjb.backend.core.security.device;

public record RiskEvaluation(
        RiskDecision decision,
        int riskScore,
        String networkLabel,
        boolean suspectNetwork,
        String reason
) {
    public static RiskEvaluation allow(int score, String network, boolean suspect, String reason) {
        return new RiskEvaluation(RiskDecision.ALLOW, score, network, suspect, reason);
    }

    public static RiskEvaluation challenge(int score, String network, boolean suspect, String reason) {
        return new RiskEvaluation(RiskDecision.CHALLENGE, score, network, suspect, reason);
    }

    public static RiskEvaluation deny(int score, String network, boolean suspect, String reason) {
        return new RiskEvaluation(RiskDecision.DENY, score, network, suspect, reason);
    }
}
