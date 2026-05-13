package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalOfficialSourceDossier(
        String subjectType,
        String subjectId,
        String affiliationId,
        String requestId,
        String organizationScope,
        String orgaoSigla,
        String unidadeCodigo,
        String publicRecognitionStatus,
        boolean sovereignRecognitionReady,
        boolean dueNow,
        Instant nextMandatoryReviewAt,
        List<String> blockingIssues,
        List<InstitutionalOfficialSourceEvidence> sources,
        List<String> fundamentos,
        Instant generatedAt
) {
}
