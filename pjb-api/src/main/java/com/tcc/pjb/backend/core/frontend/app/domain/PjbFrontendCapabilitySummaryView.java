package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendCapabilitySummaryView(
        String role,
        String papelArquitetural,
        int capabilityCount,
        boolean institutionalUser,
        boolean citizenFacing,
        List<String> capabilities
) {
}
