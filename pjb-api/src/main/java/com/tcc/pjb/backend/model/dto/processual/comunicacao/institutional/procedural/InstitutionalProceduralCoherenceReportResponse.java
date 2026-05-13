package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.time.Instant;
import java.util.List;

public record InstitutionalProceduralCoherenceReportResponse(
        boolean compliant,
        int totalFindings,
        long blockingFindings,
        List<NationalCommunicationInstitutionalProceduralCoherenceFindingResponse> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
