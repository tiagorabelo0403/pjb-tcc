package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalOfficialSourceAttestationItem(
        String sourceCode,
        String sourceLabel,
        String authority,
        String authorityScope,
        String accessMode,
        String refreshMode,
        boolean directGovernmentSource,
        boolean autoRefreshSupported,
        boolean applicable,
        boolean satisfied,
        boolean mandatoryForAutomaticActivation,
        boolean stale,
        boolean refreshRecommended,
        int confidenceScore,
        String confidenceBand,
        Instant lastVerifiedAt,
        Instant nextRefreshAt,
        String integrityHash,
        String connectorStatus,
        boolean connectorEnabled,
        boolean connectorLiveVerificationSupported,
        String connectorReferenceUrl,
        Instant connectorCheckedAt,
        Instant connectorNextCheckAt,
        List<String> connectorSignals,
        List<String> connectorBlockers,
        List<String> evidenceSignals,
        List<String> pendingIssues,
        List<String> safeNextSteps,
        List<String> fundamentos
) {
}
