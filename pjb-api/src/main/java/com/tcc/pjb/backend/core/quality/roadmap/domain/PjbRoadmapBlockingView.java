package com.tcc.pjb.backend.core.quality.roadmap.domain;

public record PjbRoadmapBlockingView(
        String scope,
        String code,
        String severity,
        String summary
) {
}
