package com.tcc.pjb.backend.model.dto.institutional;

import java.util.List;
import java.util.Map;

public record InstitutionalWorkbenchExplainabilityResponse(
        String actorBranch,
        String targetSphere,
        String verdict,
        List<String> reasons,
        List<String> warnings,
        Map<String, Object> metrics
) {
}
