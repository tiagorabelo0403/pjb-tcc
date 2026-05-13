package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record SagaTriagemSnapshot(Long rascunhoId,
                                  boolean dispatched,
                                  String status) {}
