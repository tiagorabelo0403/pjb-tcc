package com.tcc.pjb.backend.service.processual.participacao.workspace;

import java.util.List;

public record RepresentationGuardView(String status,
                                      boolean regularidadeSuficiente,
                                      boolean exigeProcuracaoFormal,
                                      boolean dispensaMandatoFuncional,
                                      String instrumentoResolvido,
                                      String regimePostulacao,
                                      List<String> documentosObrigatorios,
                                      List<String> validacoesObrigatorias,
                                      List<String> alertas) {
    public String resolvedInstrument() {
        return instrumentoResolvido;
    }
}
