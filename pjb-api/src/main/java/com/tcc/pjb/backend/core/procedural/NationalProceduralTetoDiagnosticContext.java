package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import java.util.Map;

public record NationalProceduralTetoDiagnosticContext(
        Map<String, Object> payload,
        CompetenceResolveResponse competence,
        ProceduralCanonicalResolver.CanonicalContext canonical,
        CanonicalRitoSelector.SelectedRito selectedRito
) {
}
