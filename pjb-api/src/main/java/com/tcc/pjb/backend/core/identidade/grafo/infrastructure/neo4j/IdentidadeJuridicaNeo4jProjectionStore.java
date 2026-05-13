package com.tcc.pjb.backend.core.identidade.grafo.infrastructure.neo4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.identidade.grafo.application.IdentidadeJuridicaGraphStore;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaGraphAggregate;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaPersistencia;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaProjectionBatchEntity;
import com.tcc.pjb.backend.model.repository.identity.IdentidadeJuridicaProjectionBatchRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class IdentidadeJuridicaNeo4jProjectionStore implements IdentidadeJuridicaGraphStore {

    private final ObjectStoragePort objectStoragePort;
    private final OutboxPublisher outboxPublisher;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final IdentidadeJuridicaProjectionBatchRepository projectionBatchRepository;

    public IdentidadeJuridicaNeo4jProjectionStore(ObjectProvider<ObjectStoragePort> objectStoragePortProvider,
                                                  ObjectProvider<OutboxPublisher> outboxPublisherProvider,
                                                  ObjectProvider<MeterRegistry> meterRegistryProvider,
                                                  ObjectProvider<IdentidadeJuridicaProjectionBatchRepository> projectionBatchRepositoryProvider,
                                                  ObjectMapper objectMapper) {
        this.objectStoragePort = objectStoragePortProvider.getIfAvailable();
        this.outboxPublisher = outboxPublisherProvider.getIfAvailable();
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.projectionBatchRepository = projectionBatchRepositoryProvider.getIfAvailable();
    }

    @Override
    public IdentidadeJuridicaPersistencia persistir(IdentidadeJuridicaGraphAggregate aggregate) {
        Objects.requireNonNull(aggregate, "aggregate");
        String storageKey = storageKey(aggregate);
        IdentidadeJuridicaNeo4jProjectionDocument document = document(aggregate, storageKey);
        write(storageKey, document);
        persistBatch(document);
        publish(aggregate, document);
        metrics(aggregate, document);
        return new IdentidadeJuridicaPersistencia(
                objectStoragePort == null ? "NEO4J_COMMAND_BATCH_MEMORY" : "NEO4J_COMMAND_BATCH_STORAGE",
                aggregate.vertices().size(),
                aggregate.arestas().size(),
                Instant.now()
        );
    }

    private IdentidadeJuridicaNeo4jProjectionDocument document(IdentidadeJuridicaGraphAggregate aggregate, String storageKey) {
        List<Map<String, Object>> vertices = aggregate.vertices().stream().map(this::vertexPayload).toList();
        List<Map<String, Object>> arestas = aggregate.arestas().stream().map(this::edgePayload).toList();
        String mergeFingerprint = Hashes.sha256Hex(aggregate.correlacaoId() + "#" + aggregate.vertices().size() + "#" + aggregate.arestas().size());
        return new IdentidadeJuridicaNeo4jProjectionDocument(
                aggregate.correlacaoId(),
                objectStoragePort == null ? "NEO4J_COMMAND_BATCH_MEMORY" : "NEO4J_COMMAND_BATCH_STORAGE",
                storageKey,
                mergeFingerprint,
                aggregate.vertices().size(),
                aggregate.arestas().size(),
                aggregate.geradoEm(),
                vertices,
                arestas,
                cypherStatements(aggregate),
                metadata(aggregate, mergeFingerprint)
        );
    }

    private Map<String, Object> vertexPayload(IdentidadeJuridicaVertice item) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("id", item.id());
        out.put("tipo", item.tipo().name());
        out.put("rotulo", item.rotulo());
        out.put("chaveCanonica", item.chaveCanonica());
        out.put("confianca", item.confianca());
        out.put("fontes", List.copyOf(item.fontes()));
        out.put("atributos", item.atributos());
        out.put("labels", nodeLabels(item));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> edgePayload(IdentidadeJuridicaAresta item) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("id", item.id());
        out.put("origemId", item.origemId());
        out.put("destinoId", item.destinoId());
        out.put("tipo", item.tipo().name());
        out.put("confianca", item.confianca());
        out.put("bidirecional", item.bidirecional());
        out.put("fundamentos", List.copyOf(item.fundamentos()));
        out.put("atributos", item.atributos());
        return Collections.unmodifiableMap(out);
    }

    private List<String> cypherStatements(IdentidadeJuridicaGraphAggregate aggregate) {
        ArrayList<String> statements = new ArrayList<>(aggregate.vertices().size() + aggregate.arestas().size());
        for (IdentidadeJuridicaVertice vertice : aggregate.vertices()) {
            statements.add(cypherVertice(vertice));
        }
        for (IdentidadeJuridicaAresta aresta : aggregate.arestas()) {
            statements.add(cypherAresta(aresta));
            if (aresta.bidirecional()) {
                statements.add(cypherArestaReversa(aresta));
            }
        }
        return List.copyOf(statements);
    }

    private String cypherVertice(IdentidadeJuridicaVertice vertice) {
        String labels = String.join(":", nodeLabels(vertice));
        return "MERGE (n:" + labels + " {id: $id}) SET n.tipo = $tipo, n.chaveCanonica = $chaveCanonica, n.rotulo = $rotulo, n.confianca = $confianca, n.fontes = $fontes, n.atributos = $atributos";
    }

    private String cypherAresta(IdentidadeJuridicaAresta aresta) {
        return "MATCH (o {id: $origemId}) MATCH (d {id: $destinoId}) MERGE (o)-[r:" + aresta.tipo().name() + " {id: $id}]->(d) SET r.confianca = $confianca, r.bidirecional = $bidirecional, r.fundamentos = $fundamentos, r.atributos = $atributos";
    }

    private String cypherArestaReversa(IdentidadeJuridicaAresta aresta) {
        return "MATCH (o {id: $destinoId}) MATCH (d {id: $origemId}) MERGE (o)-[r:" + aresta.tipo().name() + " {id: $idReverso}]->(d) SET r.confianca = $confianca, r.bidirecional = true, r.fundamentos = $fundamentos, r.atributos = $atributos";
    }

    private List<String> nodeLabels(IdentidadeJuridicaVertice vertice) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add("IdentidadeJuridica");
        labels.add(vertice.tipo().name());
        if (vertice.atributos().containsKey("classe")) {
            labels.add(sanitizeLabel(vertice.atributos().get("classe")));
        }
        return List.copyOf(labels);
    }

    private Map<String, Object> metadata(IdentidadeJuridicaGraphAggregate aggregate, String mergeFingerprint) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("correlacaoId", aggregate.correlacaoId());
        out.put("solicitante", aggregate.solicitante());
        out.put("origemSolicitacao", aggregate.origemSolicitacao());
        out.put("mergeFingerprint", mergeFingerprint);
        out.put("totalAchados", aggregate.achados().size());
        out.put("totalConexoesOcultas", aggregate.conexoesOcultas().size());
        out.put("fundamentos", aggregate.fundamentos());
        out.put("resumo", Map.of(
                "vertices", aggregate.resumo().totalVertices(),
                "arestas", aggregate.resumo().totalArestas(),
                "processosCorrelatos", aggregate.resumo().totalProcessosCorrelatos(),
                "riscoGlobal", aggregate.resumo().riscoGlobal().name(),
                "litigiosRepetitivos", aggregate.resumo().litigiosRepetitivosDetectados(),
                "gruposEconomicosProvaveis", aggregate.resumo().gruposEconomicosProvaveis(),
                "conflitosInteresse", aggregate.resumo().conflitosInteressePotenciais(),
                "fraudesRepresentacao", aggregate.resumo().fraudesRepresentacaoPotenciais()
        ));
        return Collections.unmodifiableMap(out);
    }

    private void write(String storageKey, IdentidadeJuridicaNeo4jProjectionDocument document) {
        if (objectStoragePort == null) {
            return;
        }
        try {
            byte[] payload = objectMapper.writeValueAsBytes(document);
            objectStoragePort.put(
                    storageKey,
                    new ByteArrayInputStream(payload),
                    payload.length,
                    "application/json",
                    Map.of(
                            "correlacaoId", document.correlacaoId(),
                            "backend", document.backend(),
                            "mergeFingerprint", document.mergeFingerprint(),
                            "totalVertices", Integer.toString(document.totalVertices()),
                            "totalArestas", Integer.toString(document.totalArestas())
                    )
            );
        } catch (Exception e) {
            throw new IllegalStateException("persistencia especializada do grafo falhou", e);
        }
    }


    private void persistBatch(IdentidadeJuridicaNeo4jProjectionDocument document) {
        if (projectionBatchRepository == null) {
            return;
        }
        try {
            IdentidadeJuridicaProjectionBatchEntity entity = new IdentidadeJuridicaProjectionBatchEntity();
            entity.setCorrelacaoId(document.correlacaoId());
            entity.setBackend(document.backend());
            entity.setStorageKey(document.storageKey());
            entity.setMergeFingerprint(document.mergeFingerprint());
            entity.setTotalVertices(document.totalVertices());
            entity.setTotalArestas(document.totalArestas());
            entity.setProjectionJson(objectMapper.writeValueAsString(document));
            entity.setCreatedAt(Instant.now());
            projectionBatchRepository.save(entity);
        } catch (Exception e) {
            throw new IllegalStateException("persistencia relacional da projecao do grafo falhou", e);
        }
    }

    private void publish(IdentidadeJuridicaGraphAggregate aggregate, IdentidadeJuridicaNeo4jProjectionDocument document) {
        if (outboxPublisher == null) {
            return;
        }
        outboxPublisher.enqueue(
                "processo.malha.identidade.grafo",
                "IDENTIDADE_JURIDICA_GRAFO_PROJETADO",
                document,
                Map.of(
                        "correlacaoId", aggregate.correlacaoId(),
                        "backend", document.backend(),
                        "storageKey", document.storageKey()
                ),
                "identidade-grafo-projecao:" + aggregate.correlacaoId() + ":" + document.mergeFingerprint(),
                "IDENTIDADE_JURIDICA_GRAFO",
                aggregate.correlacaoId()
        );
    }

    private void metrics(IdentidadeJuridicaGraphAggregate aggregate, IdentidadeJuridicaNeo4jProjectionDocument document) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("pjb.identidade.grafo.persistencia.total", "backend", document.backend()).increment();
        if (!aggregate.conexoesOcultas().isEmpty()) {
            meterRegistry.counter("pjb.identidade.grafo.persistencia.conexoes_ocultas", "backend", document.backend()).increment(aggregate.conexoesOcultas().size());
        }
    }

    private String storageKey(IdentidadeJuridicaGraphAggregate aggregate) {
        return "grafo-identidade/neo4j/" + aggregate.correlacaoId() + "/" + Hashes.sha256Hex(aggregate.correlacaoId() + '#' + aggregate.geradoEm() + '#' + aggregate.vertices().size() + '#' + aggregate.arestas().size()) + ".json";
    }

    private String sanitizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return "GENERIC";
        }
        String normalized = value.trim().replaceAll("[^A-Za-z0-9]+", "_");
        if (normalized.isBlank()) {
            return "GENERIC";
        }
        char first = normalized.charAt(0);
        if (Character.isDigit(first)) {
            return "L_" + normalized;
        }
        return normalized;
    }
}
