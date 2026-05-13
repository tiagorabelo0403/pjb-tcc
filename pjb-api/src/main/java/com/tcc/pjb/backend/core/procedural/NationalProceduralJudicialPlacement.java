package com.tcc.pjb.backend.core.procedural;

public record NationalProceduralJudicialPlacement(
        String foroSugerido,
        String cidadeSugerida,
        String ufSugerida,
        String tribunalCodigo,
        String tribunalNome,
        String varaSugerida,
        String tipoVaraSugerido,
        String judicialSystem,
        NationalProceduralDistributionSuggestion distribution,
        ProceduralForumAllocationReport forumAllocation
) {
}
