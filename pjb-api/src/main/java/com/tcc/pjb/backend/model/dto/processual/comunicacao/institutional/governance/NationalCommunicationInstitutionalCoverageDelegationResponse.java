package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalCoverageDelegationResponse(
        String snapshotId,
        String affiliationId,
        String status,
        int totalDelegations,
        int activeDelegations,
        List<NationalCommunicationInstitutionalCoverageDelegationEntryResponse> delegations,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
