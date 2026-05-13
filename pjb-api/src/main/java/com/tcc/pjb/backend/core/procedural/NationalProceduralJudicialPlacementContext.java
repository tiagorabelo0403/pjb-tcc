package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import java.util.Map;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

public record NationalProceduralJudicialPlacementContext(
        Map<String, Object> payload,
        String corpus,
        ProceduralCanonicalResolver.CanonicalContext canonical,
        CompetenceResolveResponse competence,
        TipoJustica tipoJustica,
        String ritoSugerido,
        String proceduralTrack,
        NationalProceduralActionProfile actionProfile,
        NationalProceduralJuizadoDecision juizadoDecision
) {
}
