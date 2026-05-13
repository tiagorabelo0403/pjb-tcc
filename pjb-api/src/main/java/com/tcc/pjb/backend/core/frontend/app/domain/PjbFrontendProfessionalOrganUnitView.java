package com.tcc.pjb.backend.core.frontend.app.domain;

public record PjbFrontendProfessionalOrganUnitView(
        String key,
        String label,
        String subtitle,
        String anchorCode,
        long activeProcesses,
        long activeGrants,
        long criticalQueue,
        String accentHex,
        String surfaceHex,
        String route
) {
}
