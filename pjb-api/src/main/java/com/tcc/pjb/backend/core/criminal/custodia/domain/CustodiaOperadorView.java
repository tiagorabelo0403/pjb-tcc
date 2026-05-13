package com.tcc.pjb.backend.core.criminal.custodia.domain;

public record CustodiaOperadorView(
        Long custodiaId,
        Long operadorId,
        Long magistradoId,
        String status
) {}
