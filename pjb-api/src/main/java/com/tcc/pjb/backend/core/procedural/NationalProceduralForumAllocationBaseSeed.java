package com.tcc.pjb.backend.core.procedural;

record NationalProceduralForumAllocationBaseSeed(
        NationalProceduralTerritorialAnchor territorial,
        NationalProceduralLinkageAnalysis linkage,
        String comarca,
        String uf,
        String tribunalCodigo,
        String tribunalNome,
        String unidadeCodigo,
        String varaSugerida,
        String tipoVara,
        double distributionScore
) {
}
