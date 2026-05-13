package com.tcc.pjb.backend.core.frontend.readiness.domain;

import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDeliveryBlockerView;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendRouteView;
import java.time.Instant;
import java.util.List;

public record PjbBackendReadyForFrontendBootstrapView(
        PjbBackendReadyForFrontendSummary summary,
        List<PjbBackendReadinessChecklistItem> checklist,
        PjbFrontendAuthContractView auth,
        PjbFrontendErrorContractView errors,
        List<PjbFrontendRouteView> publicRoutes,
        List<PjbFrontendDeliveryBlockerView> blockers,
        List<String> nextSteps,
        Instant generatedAt
) {
}
