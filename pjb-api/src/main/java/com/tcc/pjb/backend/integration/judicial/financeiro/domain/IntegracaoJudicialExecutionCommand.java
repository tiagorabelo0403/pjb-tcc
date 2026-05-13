package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record IntegracaoJudicialExecutionCommand(String integracao,
                                                 Long referenciaId,
                                                 int limit) {}
