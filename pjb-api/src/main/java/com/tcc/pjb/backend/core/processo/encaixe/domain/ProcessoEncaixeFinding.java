package com.tcc.pjb.backend.core.processo.encaixe.domain;

import java.util.Objects;

public record ProcessoEncaixeFinding(
        String codigo,
        String titulo,
        String eixo,
        String severidade,
        boolean bloqueante,
        String detalhe,
        String remediacao
) {
    public ProcessoEncaixeFinding {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        eixo = eixo == null ? "GERAL" : eixo;
        severidade = severidade == null ? "INFO" : severidade;
        detalhe = detalhe == null ? "" : detalhe;
        remediacao = remediacao == null ? "" : remediacao;
    }
}
