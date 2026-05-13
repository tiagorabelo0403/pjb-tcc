package com.tcc.pjb.backend.service.processual.participacao.workspace;

import java.math.BigDecimal;

public record ProcessIdentityView(Long processoId,
                                  String numeroProcesso,
                                  String tribunal,
                                  String vara,
                                  String comarca,
                                  String uf,
                                  String unidadeJudiciariaCodigo,
                                  String ramoDireito,
                                  String ritoProcessual,
                                  String faseAtual,
                                  String statusProcesso,
                                  String nivelSigilo,
                                  BigDecimal valorCausa,
                                  String parteAutora,
                                  String parteRe) {
}
