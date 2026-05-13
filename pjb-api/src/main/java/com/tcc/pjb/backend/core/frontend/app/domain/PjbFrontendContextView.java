package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendContextView(
        String tipoUsuario,
        String papelArquitetural,
        String activeHat,
        boolean authenticated,
        boolean jwtBacked,
        boolean trustedDeviceActive,
        boolean govBrLinked,
        String govBrAssuranceLevel,
        boolean govBrStepUpRequired,
        boolean frozen,
        int pendingStepCount,
        List<String> pendingSteps,
        int hatCount,
        List<String> authorities,
        String landingPath
) {
}
