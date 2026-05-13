package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialProtocolView(
        Long processoId,
        String protocolo,
        String status,
        String authzTrailId
) {}
