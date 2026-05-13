package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalOfficialSourceEvidence(
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
