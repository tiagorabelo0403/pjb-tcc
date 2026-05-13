package com.tcc.pjb.backend.model.dto.processual.substituicao.arquitetura;

import java.util.List;

public record PjbArquiteturaSubstituicaoPilarResponse(
        String codigo,
        String titulo,
        String status,
        int score,
        boolean pronto,
        List<PjbArquiteturaSubstituicaoCapacidadeResponse> capacidades,
        List<String> proximasAcoes
) {
    public PjbArquiteturaSubstituicaoPilarResponse {
        codigo = codigo == null ? "" : codigo.trim();
        titulo = titulo == null ? "" : titulo.trim();
        status = status == null ? "PENDENTE" : status.trim();
        capacidades = capacidades == null ? List.of() : List.copyOf(capacidades);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
    }
}
