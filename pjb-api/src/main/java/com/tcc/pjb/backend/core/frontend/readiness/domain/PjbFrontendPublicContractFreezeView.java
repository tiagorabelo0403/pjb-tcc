package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.time.Instant;
import java.util.List;

public record PjbFrontendPublicContractFreezeView(
        PjbFrontendPublicContractSummary summary,
        List<PjbFrontendPublicRouteContractView> routes,
        List<PjbFrontendDtoContractView> dtos,
        PjbFrontendAuthContractView auth,
        PjbFrontendErrorContractView errors,
        List<String> nextSteps,
        Instant generatedAt
) {
}
