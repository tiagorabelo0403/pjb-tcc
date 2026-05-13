package com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.PayloadMaps;

public record InstitutionalProceduralCoherenceAggregate(
        InstitutionalProceduralContextVector context,
        InstitutionalProceduralCompetenceEnvelope competenceEnvelope,
        List<InstitutionalProceduralCoherenceFinding> aggregateFindings,
        List<InstitutionalProceduralActEvaluation> actEvaluations,
        List<InstitutionalProceduralNextBestAct> nextBestActs,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalProceduralCoherenceAggregate {
        Objects.requireNonNull(context);
        Objects.requireNonNull(competenceEnvelope);
        aggregateFindings = PayloadMaps.copyListDistinct(aggregateFindings);
        actEvaluations = PayloadMaps.copyListDistinct(actEvaluations);
        nextBestActs = PayloadMaps.copyListDistinct(nextBestActs);
        fundamentos = PayloadMaps.copyDistinctStrings(fundamentos);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
