package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GruTrabalhistaConsultaResult(Long gruId, String tipo, BigDecimal valor, String status, String linhaDigitavel, LocalDate vencimento) {}
