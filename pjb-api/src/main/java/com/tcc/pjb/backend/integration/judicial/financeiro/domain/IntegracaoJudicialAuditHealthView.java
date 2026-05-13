package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialAuditHealthView(
        String integracao,
        long totalEventos,
        boolean healthy,
        String summary
) {}
