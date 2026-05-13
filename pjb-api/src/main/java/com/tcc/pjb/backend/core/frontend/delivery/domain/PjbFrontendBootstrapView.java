package com.tcc.pjb.backend.core.frontend.delivery.domain;

import java.time.Instant;
import java.util.List;

public record PjbFrontendBootstrapView(
        PjbFrontendDeliverySummary summary,
        List<PjbFrontendDomainView> domains,
        List<PjbFrontendRouteView> priorityRoutes,
        List<PjbFrontendDeliveryBlockerView> blockers,
        List<String> frontendNextSteps,
        Instant generatedAt
) {
}
