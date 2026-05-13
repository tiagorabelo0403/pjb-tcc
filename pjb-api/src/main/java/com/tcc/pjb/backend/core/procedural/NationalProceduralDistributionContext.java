package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import java.util.Map;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

record NationalProceduralDistributionContext(
        Map<String, Object> payload,
        ProceduralCanonicalResolver.CanonicalContext canonical,
        CompetenceResolveResponse competence,
        String rito,
        TipoJustica tipoJustica,
        NationalProceduralJuizadoDecision juizadoDecision,
        String cidade,
        String uf,
        NationalProceduralActionProfile actionProfile
) {
}
