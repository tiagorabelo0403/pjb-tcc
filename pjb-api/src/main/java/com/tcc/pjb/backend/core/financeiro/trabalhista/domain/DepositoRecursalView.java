package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

import java.math.BigDecimal;

public record DepositoRecursalView(Long depositoId, String instancia, String status, BigDecimal valorDepositado, String comprovanteHash) {}
