package com.tcc.pjb.backend.model.dto.consultapublica;

public record ConsultaPublicaWorkspaceDatasetDto(
        boolean personalAvailable,
        long personalProcessCount,
        int searchCacheTtlSeconds,
        int detailCacheTtlSeconds,
        int workspaceRefreshAfterSeconds,
        boolean publicSearchRequiresAuthentication,
        boolean publicSearchExposesDocuments,
        boolean regionalDisambiguationEnabled,
        boolean cpfDirectLookupEnabled,
        boolean publicActResolveEnabled,
        String sourcePolicy
) {
}
