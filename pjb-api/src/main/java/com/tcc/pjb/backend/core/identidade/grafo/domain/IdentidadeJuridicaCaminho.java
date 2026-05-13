package com.tcc.pjb.backend.core.identidade.grafo.domain;

import java.util.List;
import java.util.Objects;

public record IdentidadeJuridicaCaminho(
        String codigo,
        List<String> verticesIds,
        List<String> arestasIds,
        double confianca,
        String explicacao,
        List<String> fundamentos
) {
    public IdentidadeJuridicaCaminho {
        codigo = Objects.toString(codigo, "").trim();
        verticesIds = verticesIds == null ? List.of() : List.copyOf(verticesIds);
        arestasIds = arestasIds == null ? List.of() : List.copyOf(arestasIds);
        confianca = Math.max(0d, Math.min(1d, confianca));
        explicacao = Objects.toString(explicacao, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
