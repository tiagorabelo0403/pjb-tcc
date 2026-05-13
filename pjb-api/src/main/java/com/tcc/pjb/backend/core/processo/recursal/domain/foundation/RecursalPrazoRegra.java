package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.Objects;

public record RecursalPrazoRegra(
        String recurso,
        int diasUteis,
        boolean contaDaPostagemViaCorreio,
        boolean exigeComprovacaoFeriadoLocal,
        boolean suspendeNoRecessoForense,
        boolean admitePrazoEmDobroFazendaPublica,
        boolean admitePrazoEmDobroLitisconsortes) {

    public RecursalPrazoRegra {
        recurso = Objects.requireNonNull(recurso, "recurso").trim();
        if (recurso.isBlank()) {
            throw new IllegalArgumentException("recurso é obrigatório");
        }
        if (diasUteis <= 0) {
            throw new IllegalArgumentException("diasUteis deve ser positivo");
        }
    }
}
