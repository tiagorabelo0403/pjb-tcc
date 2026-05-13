package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalOfficialSourceDossierResponse(
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
        List<NationalCommunicationInstitutionalOfficialSourceEvidenceResponse> sources,
        List<String> fundamentos,
        Instant generatedAt
) {
}
