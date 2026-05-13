package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoConsistencyView(
        String codigoTema,
        boolean consistent,
        String summary,
        String source
) {}
