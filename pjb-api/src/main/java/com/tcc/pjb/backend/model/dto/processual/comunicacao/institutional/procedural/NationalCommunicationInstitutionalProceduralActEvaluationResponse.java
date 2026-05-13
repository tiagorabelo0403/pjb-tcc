package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.util.List;

public record NationalCommunicationInstitutionalProceduralActEvaluationResponse(
        String actionCode,
        String actionTitle,
        boolean allowed,
        boolean blocking,
        int coherenceScore,
        String decision,
        List<String> mandatoryGuards,
        List<NationalCommunicationInstitutionalProceduralCoherenceFindingResponse> findings,
        List<String> fundamentos
) {
}
