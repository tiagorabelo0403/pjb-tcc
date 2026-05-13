package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RecursalApelacaoBlueprint(
        int prazoDiasUteis,
        boolean cabivelContraSentenca,
        boolean juizAquonaoFazJuizoAdmissibilidade,
        boolean admitePreliminarContraInterlocutoriaNaoAgravavel,
        List<String> pecasObrigatorias,
        Set<RecursalPressupostoGenerico> pressupostosGenericos) {

    public RecursalApelacaoBlueprint {
        if (prazoDiasUteis <= 0) {
            throw new IllegalArgumentException("prazoDiasUteis deve ser positivo");
        }
        Objects.requireNonNull(pecasObrigatorias, "pecasObrigatorias");
        Objects.requireNonNull(pressupostosGenericos, "pressupostosGenericos");
        pecasObrigatorias = List.copyOf(pecasObrigatorias);
        pressupostosGenericos = Set.copyOf(new LinkedHashSet<>(pressupostosGenericos));
        if (pecasObrigatorias.isEmpty()) {
            throw new IllegalArgumentException("pecasObrigatorias é obrigatório");
        }
        if (pressupostosGenericos.isEmpty()) {
            throw new IllegalArgumentException("pressupostosGenericos é obrigatório");
        }
    }
}
