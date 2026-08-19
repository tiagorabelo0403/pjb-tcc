package com.tcc.pjb.backend.modules.custas.domain;

import java.math.BigDecimal;

public record CustaEmissionQuery(
        Long processoId,
        String tipoCusta,
        BigDecimal valor
) {}
