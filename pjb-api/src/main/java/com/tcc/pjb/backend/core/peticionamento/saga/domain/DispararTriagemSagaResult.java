package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record DispararTriagemSagaResult(Long rascunhoId, boolean disparada, String status, int urgencyScore, String triageStatus) {
    public DispararTriagemSagaResult(Long rascunhoId, boolean disparada, String status) {
        this(rascunhoId, disparada, status, 0, "NAO_AVALIADO");
    }
    public boolean disparado() { return disparada; }
}
