package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

public record TrabalhistaGruResult(
        Long processoId,
        String tipo,
        String linhaDigitavel,
        String status,
        String tribunalTrt
) {}
