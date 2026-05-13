package com.tcc.pjb.backend.core.identidade.grafo.domain;

public record IdentidadeJuridicaResumo(
        int totalSementes,
        int totalFontesConsultadas,
        int totalFontesDegradadas,
        int totalVertices,
        int totalArestas,
        int totalConexoesOcultas,
        int totalAchados,
        long litigantesContumazes,
        long gruposEconomicosProvaveis,
        long conflitosPotenciais,
        int totalProcessosCorrelatos,
        RiscoGlobal riscoGlobal,
        boolean litigiosRepetitivosDetectados,
        boolean conflitosInteressePotenciais,
        boolean fraudesRepresentacaoPotenciais,
        double densidade,
        String fingerprint
) {

    public enum RiscoGlobal {
        BAIXO,
        MODERADO,
        ALTO,
        CRITICO
    }
}
