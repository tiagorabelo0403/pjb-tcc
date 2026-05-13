package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoProjectionHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
