package com.tcc.pjb.backend.modules.custas.domain;

public record CustaConsistencyView(
        Long custaId,
        String status,
        boolean pixDisponivel,
        boolean gruDisponivel,
        String summary
) {}
