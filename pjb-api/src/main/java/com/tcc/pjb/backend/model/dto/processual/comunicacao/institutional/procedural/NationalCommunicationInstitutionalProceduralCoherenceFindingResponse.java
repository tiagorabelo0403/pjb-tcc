package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.util.List;

public record NationalCommunicationInstitutionalProceduralCoherenceFindingResponse(
        String code,
        String severity,
        boolean blocking,
        String message,
        List<String> evidences,
        List<String> fundamentos
) {
}
