package com.tcc.pjb.backend.core.processo.estado.domain;

import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

public sealed interface TransicaoEstadoResult
        permits TransicaoEstadoResult.Permitida, TransicaoEstadoResult.Negada {

    record Permitida(StatusProcesso estadoAnterior,
                     StatusProcesso estadoNovo) implements TransicaoEstadoResult {
    }

    record Negada(StatusProcesso estadoAtual,
                  StatusProcesso estadoSolicitado,
                  String motivo) implements TransicaoEstadoResult {
    }
}
