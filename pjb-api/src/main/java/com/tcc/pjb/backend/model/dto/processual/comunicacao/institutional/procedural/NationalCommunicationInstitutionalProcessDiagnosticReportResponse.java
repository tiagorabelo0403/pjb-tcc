package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalProcessDiagnosticReportResponse(
        boolean compliant,
        long totalFindings,
        long blockingFindings,
        List<NationalCommunicationInstitutionalProcessDiagnosticFindingResponse> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
