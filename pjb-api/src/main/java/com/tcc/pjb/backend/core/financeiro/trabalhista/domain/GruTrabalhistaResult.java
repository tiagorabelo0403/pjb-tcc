package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

import java.time.LocalDate;

public record GruTrabalhistaResult(Long gruId,
                                   String linhaDigitavel,
                                   String codigoBarras,
                                   LocalDate vencimento) {
}
