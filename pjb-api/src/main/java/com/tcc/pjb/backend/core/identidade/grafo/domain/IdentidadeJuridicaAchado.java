package com.tcc.pjb.backend.core.identidade.grafo.domain;

import java.util.List;
import java.util.Objects;

public record IdentidadeJuridicaAchado(
        String codigo,
        IdentidadeJuridicaAchadoTipo tipo,
        IdentidadeJuridicaRiscoNivel risco,
        String titulo,
        String descricao,
        List<String> verticesIds,
        List<String> arestasIds,
        List<String> modulosImpactados,
        List<String> fundamentos
) {
    public IdentidadeJuridicaAchado {
        codigo = Objects.toString(codigo, "").trim();
        tipo = Objects.requireNonNull(tipo, "tipo");
        risco = Objects.requireNonNull(risco, "risco");
        titulo = Objects.toString(titulo, "").trim();
        descricao = Objects.toString(descricao, "").trim();
        verticesIds = verticesIds == null ? List.of() : List.copyOf(verticesIds);
        arestasIds = arestasIds == null ? List.of() : List.copyOf(arestasIds);
        modulosImpactados = modulosImpactados == null ? List.of() : List.copyOf(modulosImpactados);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
