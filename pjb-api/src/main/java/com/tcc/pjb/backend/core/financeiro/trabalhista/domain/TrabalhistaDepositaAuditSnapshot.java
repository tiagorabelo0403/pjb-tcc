package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

import java.math.BigDecimal;

public record TrabalhistaDepositaAuditSnapshot(Long depositoId, String status, BigDecimal valorDepositado) {}
