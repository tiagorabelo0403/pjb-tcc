package com.tcc.pjb.backend.core.frontend.delivery.domain;

import java.util.List;

public record PjbFrontendDomainView(
        String domain,
        int routeCount,
        int controllerCount,
        boolean adminOnly,
        List<String> sampleRoutes
) {
}
