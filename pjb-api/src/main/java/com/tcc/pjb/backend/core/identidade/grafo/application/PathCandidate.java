package com.tcc.pjb.backend.core.identidade.grafo.application;

import java.util.ArrayList;
import java.util.List;

record PathCandidate(
        List<String> vertices,
        List<com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta> edges,
        double confidence) {

    int depth() {
        return edges.size();
    }

    String lastVertex() {
        return vertices.getLast();
    }

    boolean contains(String vertexId) {
        return vertices.contains(vertexId);
    }

    PathCandidate advance(String vertexId, com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta edge) {
        ArrayList<String> nextVertices = new ArrayList<>(vertices);
        nextVertices.add(vertexId);
        ArrayList<com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta> nextEdges = new ArrayList<>(edges);
        nextEdges.add(edge);
        double nextConfidence = confidence * edge.confianca();
        return new PathCandidate(List.copyOf(nextVertices), List.copyOf(nextEdges), Math.max(0d, Math.min(1d, nextConfidence)));
    }
}
