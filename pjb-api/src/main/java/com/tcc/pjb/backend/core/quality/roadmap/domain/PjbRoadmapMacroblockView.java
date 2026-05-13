package com.tcc.pjb.backend.core.quality.roadmap.domain;

public record PjbRoadmapMacroblockView(
        int number,
        String part,
        String name,
        String status,
        String operationalState,
        boolean adminSurfaceKnown
) {
}
