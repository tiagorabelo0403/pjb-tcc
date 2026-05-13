package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record RegistrarNoProcessoSagaResult(Long rascunhoId, boolean registrado, String status) {
}
