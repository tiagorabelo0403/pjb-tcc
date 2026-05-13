package com.tcc.pjb.backend.integration.judicial;

import java.util.List;

public record JudicialSubmissionCapability(
        JudicialSystem system,
        boolean enabled,
        boolean supportsProtocol,
        boolean supportsDryRun,
        boolean supportsSnapshotSync,
        boolean supportsEventSync,
        boolean requiresStepUpGovBr,
        boolean requiresCertificate,
        boolean supportsExternalMedia,
        List<String> acceptedDocumentTypes,
        List<String> acceptedRamos,
        List<String> acceptedScopes,
        String baseUrl
) {
    public JudicialSubmissionCapability {
        acceptedDocumentTypes = acceptedDocumentTypes == null ? List.of() : List.copyOf(acceptedDocumentTypes);
        acceptedRamos = acceptedRamos == null ? List.of() : List.copyOf(acceptedRamos);
        acceptedScopes = acceptedScopes == null ? List.of() : List.copyOf(acceptedScopes);
        baseUrl = baseUrl == null || baseUrl.isBlank() ? null : baseUrl.trim();
    }

    public boolean operational() {
        return enabled && supportsProtocol && baseUrl != null;
    }
}
