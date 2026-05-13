package com.tcc.pjb.backend.model.dto.institutional;

import java.util.List;
import java.util.Map;

public record InstitutionalWorkbenchActionResponse(
        String code,
        String label,
        String route,
        String method,
        boolean enabled,
        String verdict,
        String severity,
        String redirectRoute,
        List<String> reasons,
        List<String> warnings,
        Map<String, Object> metrics
) {
}
