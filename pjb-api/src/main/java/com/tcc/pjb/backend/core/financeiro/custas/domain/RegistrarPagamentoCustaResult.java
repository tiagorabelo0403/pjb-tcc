package com.tcc.pjb.backend.core.financeiro.custas.domain;

public record RegistrarPagamentoCustaResult(Long custaId, String status, boolean quitada) {
    public boolean registrado() { return quitada; }
}
