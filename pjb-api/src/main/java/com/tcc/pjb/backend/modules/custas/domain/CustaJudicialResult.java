package com.tcc.pjb.backend.modules.custas.domain;

import java.time.LocalDate;

public record CustaJudicialResult(Long custaId,
                                  boolean isento,
                                  String motivoIsencao,
                                  String linhaDigitavel,
                                  String codigoBarras,
                                  String pixPayload,
                                  LocalDate vencimento) {
    public static CustaJudicialResult isento(Long id, String motivo) {
        return new CustaJudicialResult(id, true, motivo, null, null, null, null);
    }

    public static CustaJudicialResult pendente(Long id, GruResult gru, PixResult pix, LocalDate vencimento) {
        return new CustaJudicialResult(id, false, null, gru.linhaDigitavel(), gru.codigoBarras(), pix.payload(), vencimento);
    }
}
