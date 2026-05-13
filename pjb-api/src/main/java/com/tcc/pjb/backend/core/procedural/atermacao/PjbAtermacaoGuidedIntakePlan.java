package com.tcc.pjb.backend.core.procedural.atermacao;

import java.util.List;

public record PjbAtermacaoGuidedIntakePlan(String status,
                                           PjbAtermacaoRiskLevel riskLevel,
                                           boolean canProceedToHumanReview,
                                           List<String> missingInformation,
                                           List<String> suggestedNextSteps) {
}
