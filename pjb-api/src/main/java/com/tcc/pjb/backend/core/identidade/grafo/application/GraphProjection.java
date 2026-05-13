package com.tcc.pjb.backend.core.identidade.grafo.application;

import java.util.List;
import java.util.Map;

record GraphProjection(
        List<com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice> vertices,
        List<com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta> arestas,
        Map<String, List<com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta>> adjacency,
        List<String> seedVertexIds) {
}
