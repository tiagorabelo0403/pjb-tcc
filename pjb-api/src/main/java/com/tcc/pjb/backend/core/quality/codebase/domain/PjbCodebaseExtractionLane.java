package com.tcc.pjb.backend.core.quality.codebase.domain;

import java.util.List;
import java.util.Objects;

public record PjbCodebaseExtractionLane(
        String nome,
        int arquivosMain,
        int arquivosTeste,
        double razaoTeste,
        String prontidao,
        List<String> sinais,
        List<String> acoesIniciais
) {
    public PjbCodebaseExtractionLane {
        nome = Objects.toString(nome, "").trim();
        prontidao = Objects.toString(prontidao, "PREPARAR").trim();
        sinais = sinais == null ? List.of() : List.copyOf(sinais);
        acoesIniciais = acoesIniciais == null ? List.of() : List.copyOf(acoesIniciais);
        razaoTeste = Math.max(0.0d, razaoTeste);
        arquivosMain = Math.max(0, arquivosMain);
        arquivosTeste = Math.max(0, arquivosTeste);
    }
}
