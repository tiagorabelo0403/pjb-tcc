package com.tcc.pjb.backend.core.frontend.readiness.domain;

import java.util.List;

public record PjbFrontendAuthContractView(
        boolean ready,
        boolean jwtEnabled,
        boolean statelessSession,
        boolean corsConfigured,
        boolean govBrAssuranceSurface,
        boolean idempotencyProtection,
        boolean apiExceptionHandlerPresent,
        List<String> notes
) {
}
