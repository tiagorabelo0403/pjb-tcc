package com.tcc.pjb.backend.model.dto.twin;

import java.util.List;

public record TwinRecommendationDto(
        String code,
        String title,
        String description,
        String severity,
        List<Long> evidencePrecedentIds
) {
}
