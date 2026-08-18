package com.tcc.pjb.backend.modules.custas.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record RegistrarPagamentoCustaCommand(Long custaId, BigDecimal valorPago, Instant pagoEm) {
}
