package com.tcc.pjb.backend.core.financeiro.custas.domain;

import java.math.BigDecimal;

public record CustaEmissionQuery(
        Long processoId,
        String tipoCusta,
        BigDecimal valor
) {}
