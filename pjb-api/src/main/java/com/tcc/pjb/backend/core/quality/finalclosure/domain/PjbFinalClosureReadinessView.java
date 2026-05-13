package com.tcc.pjb.backend.core.quality.finalclosure.domain;

import java.util.List;

public record PjbFinalClosureReadinessView(
        String dimension,
        String status,
        String evidence,
        List<String> nextActions
) {
}
