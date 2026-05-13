package com.tcc.pjb.backend.model.dto.admin;

import java.util.List;

public record AdminInstitutionalShardResolutionResponse(
        String clusterCode,
        String clusterLabel,
        String metadataKey,
        boolean federated,
        List<String> reasons
) {
}
