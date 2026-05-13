package com.tcc.pjb.backend.model.dto.security.authz;

import java.util.List;

public record PjbAuthorizationTrailQueryResponse(
        String sourceMode,
        int totalEntriesAvailable,
        int totalEntriesPersisted,
        int totalEntriesRuntime,
        int returned,
        PjbAuthorizationTrailSummaryResponse summary,
        List<PjbAuthorizationTrailEntryResponse> entries
) {
    public PjbAuthorizationTrailQueryResponse {
        sourceMode = sourceMode == null || sourceMode.isBlank() ? "PERSISTED" : sourceMode;
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
