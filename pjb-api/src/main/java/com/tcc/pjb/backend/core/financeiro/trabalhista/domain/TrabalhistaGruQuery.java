package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

public record TrabalhistaGruQuery(
        Long processoId,
        String tipo,
        String tribunalTrt
) {}
