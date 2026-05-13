package com.tcc.pjb.backend.core.processo.cooperacao.domain;

import java.time.Instant;
import java.util.Objects;

public record ProcessoCooperacaoInstitucionalItem(String codigo,
                                                  String destino,
                                                  String fundamento,
                                                  String prazo,
                                                  String pendencia,
                                                  String chaveCorrelacao,
                                                  Instant solicitadoEm) {
    public ProcessoCooperacaoInstitucionalItem {
        codigo = Objects.toString(codigo, "").trim();
        destino = Objects.toString(destino, "").trim();
        fundamento = Objects.toString(fundamento, "").trim();
        prazo = Objects.toString(prazo, "").trim();
        pendencia = Objects.toString(pendencia, "").trim();
        chaveCorrelacao = Objects.toString(chaveCorrelacao, "").trim();
        solicitadoEm = solicitadoEm == null ? Instant.now() : solicitadoEm;
    }
}
