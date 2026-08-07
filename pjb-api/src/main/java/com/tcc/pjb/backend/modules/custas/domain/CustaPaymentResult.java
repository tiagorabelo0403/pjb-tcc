package com.tcc.pjb.backend.modules.custas.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record CustaPaymentResult(
        Long custaId,
        String status,
        BigDecimal valorPago,
        Instant pagoEm,
        boolean liquidado
) {}
