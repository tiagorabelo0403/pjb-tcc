package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.util.Map;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

record NationalProceduralReviewSynthesisContext(
        Map<String, Object> payload,
        CanonicalRitoSelector.SelectedRito selectedRito,
        CompetenceResolveResponse competence,
        NationalProceduralActionProfile actionProfile,
        NationalProceduralJuizadoDecision juizadoDecision,
        NationalProceduralPartyProfile partyProfile,
        TetoProcessualService.DiagnosticoTetoProcessual teto,
        ProceduralForumAllocationReport forumAllocation,
        NationalProceduralDistributionSuggestion distribution,
        TipoJustica tipoJustica,
        String cidadeSugerida,
        String ufSugerida
) {

    NationalProceduralReviewSynthesisContext {
        payload = PayloadMaps.deepCopyWithoutNulls(payload);
        cidadeSugerida = trimToNull(cidadeSugerida);
        ufSugerida = trimToNull(ufSugerida);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
