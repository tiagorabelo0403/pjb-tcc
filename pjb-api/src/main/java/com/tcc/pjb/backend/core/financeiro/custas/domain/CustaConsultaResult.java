package com.tcc.pjb.backend.core.financeiro.custas.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CustaConsultaResult(Long id,
                                  String tipo,
                                  BigDecimal valor,
                                  String status,
                                  LocalDate vencimento,
                                  Instant pagoEm,
                                  BigDecimal valorPago) {
    public Long custaId() { return id; }
}
