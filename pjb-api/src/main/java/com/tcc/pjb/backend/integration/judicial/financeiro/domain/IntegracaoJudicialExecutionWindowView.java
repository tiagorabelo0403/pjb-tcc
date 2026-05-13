package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialExecutionWindowView(
        String integracao,
        long tentativas,
        boolean withinWindow,
        String summary
) {}
