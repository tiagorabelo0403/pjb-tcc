package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.Set;

public record EmbargosDivergenciaBlueprint(
        String recurso,
        int prazoDiasUteis,
        RecursalJuizoAdmissibilidadeCompetencia competencia,
        Set<String> secoesObrigatorias) {

    public static EmbargosDivergenciaBlueprint defaultBlueprint() {
        return new EmbargosDivergenciaBlueprint(
                "EMBARGOS_DIVERGENCIA",
                15,
                RecursalJuizoAdmissibilidadeCompetencia.ORGAO_AD_QUEM,
                Set.of(
                        RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                        RecursalFormalSectionLabels.DEMONSTRACAO_DIVERGENCIA,
                        RecursalFormalSectionLabels.ACORDAO_PARADIGMA,
                        RecursalFormalSectionLabels.REGULARIDADE_FORMAL
                )
        );
    }
}
