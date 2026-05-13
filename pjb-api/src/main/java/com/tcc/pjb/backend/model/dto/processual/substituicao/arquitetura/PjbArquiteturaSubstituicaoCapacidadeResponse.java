package com.tcc.pjb.backend.model.dto.processual.substituicao.arquitetura;

import java.util.List;

public record PjbArquiteturaSubstituicaoCapacidadeResponse(
        String codigo,
        String titulo,
        String status,
        int score,
        String conclusao,
        List<String> evidencias,
        List<String> pendencias
) {
    public PjbArquiteturaSubstituicaoCapacidadeResponse {
        codigo = codigo == null ? "" : codigo.trim();
        titulo = titulo == null ? "" : titulo.trim();
        status = status == null ? "PENDENTE" : status.trim();
        conclusao = conclusao == null ? "" : conclusao.trim();
        evidencias = evidencias == null ? List.of() : List.copyOf(evidencias);
        pendencias = pendencias == null ? List.of() : List.copyOf(pendencias);
    }
}
