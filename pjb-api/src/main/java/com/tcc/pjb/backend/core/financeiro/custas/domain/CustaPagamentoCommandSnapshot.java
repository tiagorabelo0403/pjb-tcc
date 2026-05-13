package com.tcc.pjb.backend.core.financeiro.custas.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record CustaPagamentoCommandSnapshot(Long custaId,
                                            BigDecimal valorPago,
                                            Instant pagoEm) {}
