package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.util.List;

public record PjbFrontendDevProfileView(
        boolean ready,
        String profile,
        boolean h2Enabled,
        boolean corsReady,
        boolean mockIntegrations,
        boolean openApiDocsPresent,
        boolean postmanPresent,
        boolean seedPackPresent,
        List<String> notes
) {
}
