package com.tcc.pjb.backend.core.procedural;

public record RegimeDistribuicaoSinais(
        boolean dependenciaOuPrevencao,
        boolean rescisoriaExcluindoJulgador,
        boolean temaRepetitivoAfetado,
        boolean precatorioOuRpv,
        boolean unidadeUnica
) {

    public static RegimeDistribuicaoSinais nenhum() {
        return new RegimeDistribuicaoSinais(false, false, false, false, false);
    }
}
