package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.util.List;

public record PjbFrontendSmokePackView(
        boolean ready,
        boolean loginFlow,
        boolean queryFlow,
        boolean timelineFlow,
        boolean protocolFlow,
        boolean custasFlow,
        boolean djeFlow,
        boolean adminDashboardsFlow,
        List<String> testClasses
) {
}
