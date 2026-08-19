package com.tcc.pjb.backend.modules.custas.domain;

public record RegistrarPagamentoCustaResult(Long custaId, String status, boolean quitada) {
    public boolean registrado() { return quitada; }
}
