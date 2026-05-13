package com.tcc.pjb.backend.model.dto.institutional;

public record InstitutionalWorkbenchRouteResponse(
        String code,
        String label,
        String route,
        String method,
        boolean primary
) {
}
