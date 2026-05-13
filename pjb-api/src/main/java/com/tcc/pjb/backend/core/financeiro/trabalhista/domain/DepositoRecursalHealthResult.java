package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;
import java.math.BigDecimal;
public record DepositoRecursalHealthResult(Long depositoId, String status, BigDecimal valorDepositado, boolean confirmado) {}
