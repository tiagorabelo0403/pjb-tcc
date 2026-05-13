package com.tcc.pjb.backend.model.dto.governance;

import java.util.List;

public record TestQualityMatrixResponse(
        int totalControllers,
        int totalProcessualServices,
        int controllerContractSuitesTarget,
        int processualIntegrationSuitesTarget,
        int criticalLoadSuitesTarget,
        List<String> criticalModules,
        List<String> structuralRisks,
        List<String> recommendations
) {
    public TestQualityMatrixResponse {
        criticalModules = criticalModules == null ? List.of() : List.copyOf(criticalModules);
        structuralRisks = structuralRisks == null ? List.of() : List.copyOf(structuralRisks);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
