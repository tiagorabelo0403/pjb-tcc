package com.tcc.pjb.backend.core.procedural;

record NationalProceduralConfidenceAssessment(
        double confidence,
        boolean requiresHumanReview,
        String riskLevel
) {
}
