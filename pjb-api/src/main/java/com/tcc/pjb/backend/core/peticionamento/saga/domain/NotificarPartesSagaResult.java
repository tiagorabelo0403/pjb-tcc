package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record NotificarPartesSagaResult(Long rascunhoId, boolean notificadas, String status) {
    public boolean notificado() { return notificadas; }
}
