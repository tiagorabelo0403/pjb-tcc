package com.tcc.pjb.backend.core.financeiro.custas.domain;

public record PixCobrancaView(String txid,
                              String payload,
                              boolean pendente) {
}
