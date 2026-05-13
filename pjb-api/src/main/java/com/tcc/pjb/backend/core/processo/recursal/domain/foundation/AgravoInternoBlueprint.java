package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.Set;

public record AgravoInternoBlueprint(
        String recurso,
        int prazoDiasUteis,
        RecursalJuizoAdmissibilidadeCompetencia competencia,
        Set<String> secoesObrigatorias) {

    public static AgravoInternoBlueprint defaultBlueprint() {
        return new AgravoInternoBlueprint(
                "AGRAVO_INTERNO",
                15,
                RecursalJuizoAdmissibilidadeCompetencia.ORGAO_AD_QUEM,
                Set.of(
                        RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                        RecursalFormalSectionLabels.RAZOES_RECURSAIS,
                        RecursalFormalSectionLabels.TEMPESTIVIDADE
                )
        );
    }
}
