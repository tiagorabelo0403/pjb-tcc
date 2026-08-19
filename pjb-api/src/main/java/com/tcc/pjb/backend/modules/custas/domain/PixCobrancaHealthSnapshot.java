package com.tcc.pjb.backend.modules.custas.domain;

public record PixCobrancaHealthSnapshot(Long custaId, String txid, boolean pendente) {
    public boolean pending() { return pendente; }
}
