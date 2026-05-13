package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse(
        NationalCommunicationInstitutionalProceduralContextVectorResponse context,
        NationalCommunicationInstitutionalProceduralCompetenceEnvelopeResponse competenceEnvelope,
        List<NationalCommunicationInstitutionalProceduralCoherenceFindingResponse> aggregateFindings,
        List<NationalCommunicationInstitutionalProceduralActEvaluationResponse> actEvaluations,
        List<NationalCommunicationInstitutionalProceduralNextBestActResponse> nextBestActs,
        List<String> fundamentos,
        Instant generatedAt
) {
}
