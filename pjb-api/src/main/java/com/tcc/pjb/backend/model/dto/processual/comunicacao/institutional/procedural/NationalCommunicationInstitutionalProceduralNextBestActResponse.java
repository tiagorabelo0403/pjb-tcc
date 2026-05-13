package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.util.List;

public record NationalCommunicationInstitutionalProceduralNextBestActResponse(
        String actionCode,
        String actionTitle,
        int priorityScore,
        String rationale,
        List<String> expectedGuards,
        List<String> fundamentos
) {
}
