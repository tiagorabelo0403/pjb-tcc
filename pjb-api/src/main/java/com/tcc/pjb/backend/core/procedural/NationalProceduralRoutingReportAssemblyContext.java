package com.tcc.pjb.backend.core.procedural;

import java.util.Map;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

public record NationalProceduralRoutingReportAssemblyContext(
        NationalProceduralActionProfile actionProfile,
        String proceduralRegime,
        String proceduralTrack,
        String tipoJustica,
        String ritoSugerido,
        String tribunalCodigo,
        String tribunalNome,
        String judicialSystem,
        String foroSugerido,
        String cidadeSugerida,
        String ufSugerida,
        String varaSugerida,
        String tipoVaraSugerido,
        String complexityBand,
        String probatoryProfile,
        NationalProceduralJuizadoDecision juizadoDecision,
        NationalProceduralReviewSynthesis reviewSynthesis,
        ProceduralEconomicGateReport economicGate,
        ProceduralForumAllocationReport forumAllocation,
        Map<String, Object> metadata
) {
}
