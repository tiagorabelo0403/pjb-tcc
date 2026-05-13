package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DepositoRecursalResult(Long depositoId,
                                     BigDecimal valorDepositado,
                                     LocalDate dataDeposito,
                                     String status) {
}
