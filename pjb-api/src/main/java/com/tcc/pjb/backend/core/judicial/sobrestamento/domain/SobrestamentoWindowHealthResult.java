package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

public record SobrestamentoWindowHealthResult(
        String codigoTema,
        String resultado,
        boolean healthy,
        String summary
) {}
