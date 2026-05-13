package com.tcc.pjb.backend.core.financeiro.custas.domain;

import java.time.LocalDate;

public record CustaJudicialView(Long custaId,
                                String tipo,
                                String status,
                                String linhaDigitavel,
                                String pixPayload,
                                LocalDate vencimento) {
    public Long id() { return custaId; }
}
