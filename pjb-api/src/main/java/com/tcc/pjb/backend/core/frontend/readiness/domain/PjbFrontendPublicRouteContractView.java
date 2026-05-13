package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.util.List;

public record PjbFrontendPublicRouteContractView(
        String method,
        String path,
        String controller,
        String domain,
        String authMode,
        String requestBodyType,
        String responseType,
        boolean stableForFrontend,
        List<String> notes
) {
}
