package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialHealthQuery(
        String integracao,
        String status,
        Long processoId
) {}
