package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalOfficialIdentifierDossierResponse(
        String subjectType,
        String subjectId,
        String affiliationId,
        String requestId,
        String organizationScope,
        String orgaoSigla,
        String unidadeCodigo,
        String overallStatus,
        boolean materialEvidenceReady,
        Instant generatedAt,
        List<String> blockingIssues,
        List<NationalCommunicationInstitutionalOfficialIdentifierCheckResponse> checks,
        List<String> fundamentos,
        String integrityHash
) {
}
