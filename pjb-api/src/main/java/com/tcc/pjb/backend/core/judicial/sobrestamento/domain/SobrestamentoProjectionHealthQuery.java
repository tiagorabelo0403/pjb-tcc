package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoProjectionHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
