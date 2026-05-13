package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialWindowHealthView(
        int pending,
        int failed,
        int confirmed,
        boolean healthy
) {}
