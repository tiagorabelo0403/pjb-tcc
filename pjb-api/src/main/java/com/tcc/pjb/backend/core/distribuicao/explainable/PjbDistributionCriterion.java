package com.tcc.pjb.backend.core.distribuicao.explainable;

import java.util.Objects;

public record PjbDistributionCriterion(
        String code,
        String description,
        double weight,
        boolean blocking,
        boolean satisfied
) {
    public PjbDistributionCriterion {
        code = Objects.toString(code, "").trim().toUpperCase();
        description = Objects.toString(description, "").trim();
        weight = Math.max(0.0d, Math.min(1.0d, weight));
    }
}
