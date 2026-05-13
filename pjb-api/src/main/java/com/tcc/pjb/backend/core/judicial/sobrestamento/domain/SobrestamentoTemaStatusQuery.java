package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoTemaStatusQuery(
        String reference,
        String scope,
        Integer limit
) {
}
