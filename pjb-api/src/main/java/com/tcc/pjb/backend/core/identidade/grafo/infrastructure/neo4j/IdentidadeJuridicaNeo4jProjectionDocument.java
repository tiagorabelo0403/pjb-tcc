package com.tcc.pjb.backend.core.identidade.grafo.infrastructure.neo4j;

import java.util.Collections;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record IdentidadeJuridicaNeo4jProjectionDocument(
        String correlacaoId,
        String backend,
        String storageKey,
        String mergeFingerprint,
        int totalVertices,
        int totalArestas,
        Instant generatedAt,
        List<Map<String, Object>> vertices,
        List<Map<String, Object>> arestas,
        List<String> cypherStatements,
        Map<String, Object> metadata
) {
    public IdentidadeJuridicaNeo4jProjectionDocument {
        backend = backend == null || backend.isBlank() ? "NEO4J_COMMAND_BATCH" : backend;
        storageKey = storageKey == null ? "" : storageKey.trim();
        mergeFingerprint = mergeFingerprint == null ? "" : mergeFingerprint.trim();
        totalVertices = Math.max(0, totalVertices);
        totalArestas = Math.max(0, totalArestas);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        vertices = vertices == null ? List.of() : List.copyOf(vertices);
        arestas = arestas == null ? List.of() : List.copyOf(arestas);
        cypherStatements = cypherStatements == null ? List.of() : List.copyOf(cypherStatements);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }
}
