package com.tcc.pjb.backend.core.identidade.grafo.application;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class IdentidadeJuridicaArestaAccumulator {

    private final String id;
    private final String origemId;
    private final String destinoId;
    private final com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaArestaTipo tipo;
    private double confianca;
    private final boolean bidirecional;
    private final LinkedHashSet<String> fundamentos;
    private final LinkedHashMap<String, String> atributos;

    IdentidadeJuridicaArestaAccumulator(com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta source) {
        this.id = source.id();
        this.origemId = source.origemId();
        this.destinoId = source.destinoId();
        this.tipo = source.tipo();
        this.confianca = source.confianca();
        this.bidirecional = source.bidirecional();
        this.fundamentos = new LinkedHashSet<>(source.fundamentos());
        this.atributos = new LinkedHashMap<>(source.atributos());
    }

    IdentidadeJuridicaArestaAccumulator merge(com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta source) {
        confianca = Math.max(confianca, source.confianca());
        fundamentos.addAll(source.fundamentos());
        atributos.putAll(source.atributos());
        return this;
    }

    com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta toAresta() {
        return new com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta(id, origemId, destinoId, tipo, confianca, bidirecional, Set.copyOf(fundamentos), Map.copyOf(atributos));
    }
}
