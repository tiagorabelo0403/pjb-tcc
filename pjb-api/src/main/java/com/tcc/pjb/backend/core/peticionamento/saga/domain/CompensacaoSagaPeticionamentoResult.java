package com.tcc.pjb.backend.core.peticionamento.saga.domain;

public record CompensacaoSagaPeticionamentoResult(Long rascunhoId,
                                                  String statusFinal) {
    public String status() { return statusFinal; }
}
