package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialPayloadHashView(
        Long processoId,
        String protocolo,
        String status,
        String tipo
) {}
