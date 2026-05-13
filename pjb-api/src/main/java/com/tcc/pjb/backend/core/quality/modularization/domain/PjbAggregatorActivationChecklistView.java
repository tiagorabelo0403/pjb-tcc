package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.List;

public record PjbAggregatorActivationChecklistView(
        String code,
        String status,
        String summary,
        List<String> evidence) {
}
