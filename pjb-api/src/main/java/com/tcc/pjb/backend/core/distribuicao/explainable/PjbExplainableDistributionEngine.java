package com.tcc.pjb.backend.core.distribuicao.explainable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class PjbExplainableDistributionEngine {

    public PjbExplainableDistributionDecision decide(String selectedUnitCode, List<PjbDistributionCriterion> criteria) {
        List<PjbDistributionCriterion> normalized = criteria == null ? List.of() : List.copyOf(criteria);
        LinkedHashSet<String> explanation = new LinkedHashSet<>();
        double score = 0.50d;
        boolean review = false;
        for (PjbDistributionCriterion criterion : normalized) {
            if (criterion.blocking() && !criterion.satisfied()) {
                review = true;
                explanation.add("critério bloqueante não satisfeito: " + firstNonBlank(criterion.description(), criterion.code()));
                score -= 0.25d;
            } else if (criterion.satisfied()) {
                score += criterion.weight() * 0.20d;
                explanation.add("critério aplicado: " + firstNonBlank(criterion.description(), criterion.code()));
            } else {
                score -= criterion.weight() * 0.10d;
                explanation.add("critério exige atenção: " + firstNonBlank(criterion.description(), criterion.code()));
            }
        }
        if (Objects.toString(selectedUnitCode, "").isBlank()) {
            review = true;
            explanation.add("unidade destino ausente");
            score = Math.min(score, 0.40d);
        }
        return new PjbExplainableDistributionDecision(selectedUnitCode, round(score), review, normalized, List.copyOf(explanation));
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private double round(double value) {
        return BigDecimal.valueOf(Math.max(0.0d, Math.min(1.0d, value))).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
