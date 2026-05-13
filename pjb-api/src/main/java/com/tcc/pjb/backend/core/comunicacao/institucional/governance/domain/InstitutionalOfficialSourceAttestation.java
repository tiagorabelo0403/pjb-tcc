package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalOfficialSourceAttestation(
        String subjectType,
        String subjectId,
        String affiliationId,
        String requestId,
        String organizationScope,
        String orgaoSigla,
        String unidadeCodigo,
        String publicRecognitionStatus,
        String attestationStatus,
        boolean sovereignRecognitionReady,
        boolean dueNow,
        boolean automaticRefreshEligible,
        Instant lastAttestedAt,
        Instant nextRefreshAt,
        List<String> blockingIssues,
        List<InstitutionalOfficialSourceAttestationItem> sources,
        List<String> fundamentos,
        String integrityHash
) {
}
