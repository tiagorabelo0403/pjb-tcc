package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record RenajudRestricaoRequest(Long processoId,
                                      String tipo,
                                      String placa,
                                      String renavam) {
}
