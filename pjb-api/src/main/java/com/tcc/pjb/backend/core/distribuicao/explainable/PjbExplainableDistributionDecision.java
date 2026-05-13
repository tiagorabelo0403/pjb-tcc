package com.tcc.pjb.backend.core.distribuicao.explainable;

import java.util.List;
import java.util.Objects;

public record PjbExplainableDistributionDecision(
        String selectedUnitCode,
        double confidence,
        boolean humanReviewRequired,
        List<PjbDistributionCriterion> criteria,
        List<String> explanation
) {
    public PjbExplainableDistributionDecision {
        selectedUnitCode = Objects.toString(selectedUnitCode, "").trim().toUpperCase();
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        explanation = explanation == null ? List.of() : List.copyOf(explanation);
    }
}
