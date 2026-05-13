package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalOfficialSourceEvidenceResponse(
        String sourceCode,
        String sourceLabel,
        String sourceGroup,
        boolean applicable,
        boolean satisfied,
        boolean mandatoryForAutomaticActivation,
        boolean stale,
        Instant lastEvidenceAt,
        Instant nextReviewAt,
        List<String> evidenceSignals,
        List<String> pendingIssues,
        List<String> fundamentos
) {
}
