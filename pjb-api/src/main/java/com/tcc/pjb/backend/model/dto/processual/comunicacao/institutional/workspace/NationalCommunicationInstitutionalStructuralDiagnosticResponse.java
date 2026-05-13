package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalStructuralDiagnosticResponse(
        String affiliationId,
        boolean compliant,
        long totalFindings,
        long blockingFindings,
        List<NationalCommunicationInstitutionalStructuralDiagnosticFindingResponse> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
