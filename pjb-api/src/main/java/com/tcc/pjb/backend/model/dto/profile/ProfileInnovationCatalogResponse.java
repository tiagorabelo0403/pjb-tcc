package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;

public record ProfileInnovationCatalogResponse(
        String role,
        String resolvedFrom,
        List<String> roleMatrixCapabilities,
        List<ProfileImplementedCapabilityDto> implementedCapabilities,
        List<String> availableRoles,
        Instant generatedAt
) {
}
