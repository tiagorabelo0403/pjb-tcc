package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialOperadorView(
        Long processoId,
        Long operadorId,
        String tipo,
        String status
) {}
