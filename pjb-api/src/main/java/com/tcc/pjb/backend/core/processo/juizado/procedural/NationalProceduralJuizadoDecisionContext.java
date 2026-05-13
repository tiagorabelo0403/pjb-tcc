package com.tcc.pjb.backend.core.processo.juizado.procedural;

import com.tcc.pjb.backend.core.procedural.NationalProceduralActionProfile;
import com.tcc.pjb.backend.core.procedural.NationalProceduralPartyProfile;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.util.Map;

record NationalProceduralJuizadoDecisionContext(
        Map<String, Object> payload,
        CompetenceResolveResponse competence,
        NationalProceduralActionProfile actionProfile,
        NationalProceduralPartyProfile partyProfile,
        TetoProcessualService.DiagnosticoTetoProcessual teto,
        String corpus
) {
}
