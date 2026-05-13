package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.Set;

public record AgravoRecursoExcepcionalBlueprint(
        String recurso,
        int prazoDiasUteis,
        RecursalJuizoAdmissibilidadeCompetencia competencia,
        Set<String> secoesObrigatorias) {

    public static AgravoRecursoExcepcionalBlueprint defaultBlueprint() {
        return new AgravoRecursoExcepcionalBlueprint(
                "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO",
                15,
                RecursalJuizoAdmissibilidadeCompetencia.BIPARTIDO_TRIBUNAL_E_CORTE_SUPERIOR,
                Set.of(
                        RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                        RecursalFormalSectionLabels.IMPUGNACAO_INADMISSIBILIDADE,
                        RecursalFormalSectionLabels.TEMPESTIVIDADE,
                        RecursalFormalSectionLabels.PREPARO
                )
        );
    }
}
