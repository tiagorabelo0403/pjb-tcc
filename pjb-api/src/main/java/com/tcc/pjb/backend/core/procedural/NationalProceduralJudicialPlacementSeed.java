package com.tcc.pjb.backend.core.procedural;

record NationalProceduralJudicialPlacementSeed(
        String cidadeSugerida,
        String ufSugerida,
        String tribunalCodigo,
        String tribunalNome,
        String varaSugerida,
        String tipoVaraSugerido,
        String judicialSystem,
        NationalProceduralDistributionSuggestion distribution
) {
}
