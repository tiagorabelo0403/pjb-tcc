package com.tcc.pjb.backend.model.dto.governance;

import java.util.List;

public record StructuralAutoRemediationReportResponse(
        int controllersScanned,
        int servicesScanned,
        int duplicateHttpPaths,
        int rawResponseEndpoints,
        int requestBodiesWithoutValidation,
        int processualServicesWithoutController,
        List<String> duplicatePathMappings,
        List<String> rawResponseEndpointOwners,
        List<String> requestBodiesMissingValidation,
        List<String> servicesWithoutController,
        List<String> remediationPriorities
) {
    public StructuralAutoRemediationReportResponse {
        duplicatePathMappings = duplicatePathMappings == null ? List.of() : List.copyOf(duplicatePathMappings);
        rawResponseEndpointOwners = rawResponseEndpointOwners == null ? List.of() : List.copyOf(rawResponseEndpointOwners);
        requestBodiesMissingValidation = requestBodiesMissingValidation == null ? List.of() : List.copyOf(requestBodiesMissingValidation);
        servicesWithoutController = servicesWithoutController == null ? List.of() : List.copyOf(servicesWithoutController);
        remediationPriorities = remediationPriorities == null ? List.of() : List.copyOf(remediationPriorities);
    }
}
