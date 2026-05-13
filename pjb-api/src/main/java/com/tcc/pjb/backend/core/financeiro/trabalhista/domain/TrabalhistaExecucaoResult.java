package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

public record TrabalhistaExecucaoResult(
        Long processoId,
        String status,
        String tribunalTrt,
        boolean emExecucao
) {}
