package com.tcc.pjb.backend.service.semantic;

import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;

public record SemanticRankedHit(
        Precedente precedente,
        float vectorScore,
        double lexicalScore,
        double contextualScore,
        double finalScore
) {
}
