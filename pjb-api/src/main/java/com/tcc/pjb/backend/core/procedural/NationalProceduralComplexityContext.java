package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.util.Map;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

record NationalProceduralComplexityContext(
        NationalProceduralActionProfile actionProfile,
        String probatoryProfile,
        NationalProceduralPartyProfile partyProfile,
        Map<String, Object> payload,
        TetoProcessualService.DiagnosticoTetoProcessual teto,
        NationalProceduralJuizadoDecision juizadoDecision
) {
}
