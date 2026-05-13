package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DepositoRecursalConsultaResult(Long depositoId, String instancia, BigDecimal valorTeto, BigDecimal valorDepositado, String status, LocalDate dataDeposito) {}
