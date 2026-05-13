package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.tribunal.distribuicao.ConfiguracaoDistribuicaoVaraService;

record NationalProceduralForumAllocationSeed(
        TpuClasseCnj classeTpu,
        NationalProceduralTerritorialAnchor territorial,
        NationalProceduralLinkageAnalysis linkage,
        String comarca,
        String uf,
        String tribunalCodigo,
        String tribunalNome,
        String unidadeCodigo,
        String varaSugerida,
        String tipoVara,
        double distributionScore,
        ConfiguracaoDistribuicaoVaraService.PerfilVara perfil
) {

    NationalProceduralForumAllocationSeed {
        comarca = trimToNull(comarca);
        uf = trimToNull(uf);
        tribunalCodigo = trimToNull(tribunalCodigo);
        tribunalNome = trimToNull(tribunalNome);
        unidadeCodigo = trimToNull(unidadeCodigo);
        varaSugerida = trimToNull(varaSugerida);
        tipoVara = trimToNull(tipoVara);
        distributionScore = Math.max(0.0d, distributionScore);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
