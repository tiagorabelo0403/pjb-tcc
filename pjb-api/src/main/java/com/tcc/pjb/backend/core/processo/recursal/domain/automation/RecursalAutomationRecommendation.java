package com.tcc.pjb.backend.core.processo.recursal.domain.automation;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record RecursalAutomationRecommendation(
        String recurso,
        int prioridade,
        String fundamentoBase,
        int prazoDiasUteis,
        String juizoAdmissibilidadeCompetencia,
        String meritoErroTipoSugerido,
        Set<String> secoesObrigatorias,
        boolean subordinadoAoPrincipal) {

    public RecursalAutomationRecommendation {
        recurso = Objects.requireNonNull(recurso, "recurso").trim();
        fundamentoBase = Objects.requireNonNull(fundamentoBase, "fundamentoBase").trim();
        juizoAdmissibilidadeCompetencia = Objects.requireNonNull(juizoAdmissibilidadeCompetencia, "juizoAdmissibilidadeCompetencia").trim();
        meritoErroTipoSugerido = Objects.requireNonNull(meritoErroTipoSugerido, "meritoErroTipoSugerido").trim();
        Objects.requireNonNull(secoesObrigatorias, "secoesObrigatorias");
        secoesObrigatorias = Set.copyOf(new LinkedHashSet<>(secoesObrigatorias));
        if (recurso.isBlank()) {
            throw new IllegalArgumentException("recurso é obrigatório");
        }
        if (fundamentoBase.isBlank()) {
            throw new IllegalArgumentException("fundamentoBase é obrigatório");
        }
        if (juizoAdmissibilidadeCompetencia.isBlank()) {
            throw new IllegalArgumentException("juizoAdmissibilidadeCompetencia é obrigatório");
        }
        if (meritoErroTipoSugerido.isBlank()) {
            throw new IllegalArgumentException("meritoErroTipoSugerido é obrigatório");
        }
        if (prioridade <= 0) {
            throw new IllegalArgumentException("prioridade deve ser positiva");
        }
        if (prazoDiasUteis <= 0) {
            throw new IllegalArgumentException("prazoDiasUteis deve ser positivo");
        }
        if (secoesObrigatorias.isEmpty()) {
            throw new IllegalArgumentException("secoesObrigatorias é obrigatório");
        }
    }
}
