package com.tcc.pjb.backend.core.quality.apisurface.domain;

import java.time.Instant;
import java.util.List;

public record PjbApiSurfaceSanityAggregate(
        boolean raizEncontrada,
        boolean limpo,
        int controllersInspecionados,
        int dtoInspecionados,
        int rotasDuplicadas,
        int dtoForaDoPadrao,
        int entidadesExpostasDiretamente,
        List<PjbApiSurfaceIssue> issues,
        Instant auditadoEm
) {
    public int controllers() {
        return controllersInspecionados;
    }

    public int score() {
        int score = 100;
        score -= Math.min(45, rotasDuplicadas * 20);
        score -= Math.min(20, dtoForaDoPadrao * 5);
        score -= Math.min(20, entidadesExpostasDiretamente * 4);
        score -= Math.min(15, issues.size());
        return Math.max(0, score);
    }
}
