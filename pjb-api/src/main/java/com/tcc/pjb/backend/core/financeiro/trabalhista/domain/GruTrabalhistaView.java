package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

import java.time.LocalDate;

public record GruTrabalhistaView(Long gruId, String tipo, String status, String linhaDigitavel, LocalDate vencimento) {}
