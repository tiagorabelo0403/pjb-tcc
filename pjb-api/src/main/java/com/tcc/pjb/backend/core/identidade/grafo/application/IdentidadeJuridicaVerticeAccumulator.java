package com.tcc.pjb.backend.core.identidade.grafo.application;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class IdentidadeJuridicaVerticeAccumulator {

    private final String id;
    private final com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVerticeTipo tipo;
    private final String chaveCanonica;
    private String rotulo;
    private double confianca;
    private final LinkedHashSet<String> fontes;
    private final LinkedHashMap<String, String> atributos;

    IdentidadeJuridicaVerticeAccumulator(com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice source) {
        this.id = source.id();
        this.tipo = source.tipo();
        this.chaveCanonica = source.chaveCanonica();
        this.rotulo = source.rotulo();
        this.confianca = source.confianca();
        this.fontes = new LinkedHashSet<>(source.fontes());
        this.atributos = new LinkedHashMap<>(source.atributos());
    }

    IdentidadeJuridicaVerticeAccumulator merge(com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice source) {
        if (source.rotulo() != null && source.rotulo().length() > rotulo.length()) {
            rotulo = source.rotulo();
        }
        confianca = Math.max(confianca, source.confianca());
        fontes.addAll(source.fontes());
        atributos.putAll(source.atributos());
        return this;
    }

    com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice toVertice() {
        return new com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice(id, tipo, chaveCanonica, rotulo, confianca, Set.copyOf(fontes), Map.copyOf(atributos));
    }
}
