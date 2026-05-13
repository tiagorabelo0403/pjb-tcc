package com.tcc.pjb.backend.model.dto.governance;

import java.util.List;

public record StructuralGovernanceReportResponse(
        int totalControllers,
        int totalServices,
        int totalRepositories,
        int totalControllersSemPreAuthorize,
        int totalControllersSemRequestMapping,
        List<String> controllersSemPreAuthorize,
        List<String> controllersSemRequestMapping,
        List<String> destaques
) {
}
