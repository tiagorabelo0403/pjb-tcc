package com.tcc.pjb.backend.core.identidade.grafo.application;

import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAchado;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAchadoTipo;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaAresta;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaArestaTipo;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaCaminho;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaChaveTipo;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaConsulta;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaGraphAggregate;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaGraphAtualizadoEvent;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaPersistencia;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaResumo;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaRiscoNivel;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaSemente;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaSnapshot;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVertice;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVerticeTipo;
import com.tcc.pjb.backend.core.util.DeterministicUuid;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.service.DomainEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class IdentidadeJuridicaGraphApplicationService {

    private static final java.time.Duration FONTE_RESOLUTION_TIMEOUT = java.time.Duration.ofSeconds(4);

    private static final List<String> MODULOS_BASE = List.of(
            "core/processo/unificado",
            "core/processo/parte",
            "core/processo/competencia",
            "core/processo/distribuicao",
            "core/processo/prevencao",
            "core/processo/conexao",
            "core/processo/dependencia",
            "core/governance",
            "ai/jurimetria"
    );

    private final List<IdentidadeJuridicaFontePort> fontes;
    private final IdentidadeJuridicaGraphStore graphStore;
    private final DecisionTraceService decisionTraceService;
    private final DomainEventPublisher domainEventPublisher;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final PjbExecutionOrchestrator executionOrchestrator;

    public IdentidadeJuridicaGraphApplicationService(List<IdentidadeJuridicaFontePort> fontes,
                                                     ObjectProvider<IdentidadeJuridicaGraphStore> graphStoreProvider,
                                                     ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                                     ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                                                     ObjectProvider<MeterRegistry> meterRegistryProvider,
                                                     ObjectMapper objectMapper,
                                                     PjbExecutionOrchestrator executionOrchestrator) {
        this.fontes = fontes == null ? List.of() : fontes.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(IdentidadeJuridicaFontePort::prioridade)
                        .thenComparing(IdentidadeJuridicaFontePort::codigoFonte))
                .toList();
        this.graphStore = graphStoreProvider.getIfAvailable();
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.domainEventPublisher = domainEventPublisherProvider.getIfAvailable();
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator);
    }

    public IdentidadeJuridicaGraphAggregate analisar(IdentidadeJuridicaConsulta consulta) {
        Instant startedAt = Instant.now();
        IdentidadeJuridicaConsulta normalized = normalize(consulta);
        List<IdentidadeJuridicaSnapshot> snapshots = resolverSnapshots(normalized);
        GraphProjection projection = projetar(normalized, snapshots);
        List<IdentidadeJuridicaCaminho> conexoesOcultas = detectarConexoesOcultas(normalized, projection);
        List<IdentidadeJuridicaAchado> achados = detectarAchados(normalized, projection, snapshots, conexoesOcultas);
        String fingerprint = fingerprint(projection.vertices(), projection.arestas(), achados, conexoesOcultas);
        IdentidadeJuridicaResumo resumo = resumir(normalized, snapshots, projection, conexoesOcultas, achados, fingerprint);
        List<String> fundamentos = consolidarFundamentos(normalized, snapshots, achados, conexoesOcultas, resumo);
        IdentidadeJuridicaGraphAggregate aggregate = new IdentidadeJuridicaGraphAggregate(
                normalized.correlacaoId(),
                normalized.solicitante(),
                normalized.origemSolicitacao(),
                projection.vertices(),
                projection.arestas(),
                conexoesOcultas,
                achados,
                resumo,
                null,
                fundamentos,
                Instant.now(),
                Duration.between(startedAt, Instant.now())
        );
        IdentidadeJuridicaPersistencia persistencia = persistir(normalized, aggregate);
        IdentidadeJuridicaGraphAggregate finalAggregate = new IdentidadeJuridicaGraphAggregate(
                aggregate.correlacaoId(),
                aggregate.solicitante(),
                aggregate.origemSolicitacao(),
                aggregate.vertices(),
                aggregate.arestas(),
                aggregate.conexoesOcultas(),
                aggregate.achados(),
                aggregate.resumo(),
                persistencia,
                aggregate.fundamentos(),
                aggregate.geradoEm(),
                aggregate.tempoProcessamento()
        );
        registrarExplainability(finalAggregate, normalized, snapshots);
        publicarEvento(normalized, finalAggregate);
        registrarMetricas(finalAggregate);
        return finalAggregate;
    }

    private IdentidadeJuridicaConsulta normalize(IdentidadeJuridicaConsulta consulta) {
        Objects.requireNonNull(consulta, "consulta");
        String correlacaoId = blankToNull(consulta.correlacaoId());
        if (correlacaoId == null) {
            correlacaoId = DeterministicUuid.v5(
                    "pjb-identidade-graph",
                    consulta.sementes().stream().map(seed -> seed.tipo() + ":" + canonicalize(seed.tipo(), seed.valor())).sorted().collect(Collectors.joining("|"))
                            + "#"
                            + consulta.processosRaiz().stream().map(this::normalizeProcessNumber).sorted().collect(Collectors.joining("|"))
            ).toString();
        }
        List<IdentidadeJuridicaSemente> sementes = consulta.sementes().stream()
                .map(this::normalizeSeed)
                .distinct()
                .toList();
        List<String> processos = consulta.processosRaiz().stream()
                .map(this::normalizeProcessNumber)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        return new IdentidadeJuridicaConsulta(
                correlacaoId,
                consulta.solicitante(),
                sementes,
                processos,
                consulta.profundidadeMaxima(),
                consulta.limiteVertices(),
                consulta.limiteArestas(),
                consulta.persistirNoGrafo(),
                consulta.publicarEvento(),
                consulta.origemSolicitacao()
        );
    }

    private IdentidadeJuridicaSemente normalizeSeed(IdentidadeJuridicaSemente seed) {
        String canonical = canonicalize(seed.tipo(), seed.valor());
        return new IdentidadeJuridicaSemente(seed.tipo(), canonical, seed.rotulo(), seed.polo(), seed.atributos());
    }

    private List<IdentidadeJuridicaSnapshot> resolverSnapshots(IdentidadeJuridicaConsulta consulta) {
        if (fontes.isEmpty()) {
            return List.of(seedOnlySnapshot(consulta));
        }
        List<CompletableFuture<IdentidadeJuridicaSnapshot>> futures = fontes.stream()
                .filter(fonte -> fonte.suporta(consulta))
                .map(fonte -> executionOrchestrator.supply(
                                PjbExecutionDescriptor.externalIo("identidade.grafo.snapshot." + sanitizeOperationSegment(fonte.codigoFonte()), FONTE_RESOLUTION_TIMEOUT),
                                () -> resolverFonte(fonte, consulta))
                        .exceptionally(ex -> failureSnapshot(fonte, ex)))
                .toList();
        awaitSnapshots(futures);
        ArrayList<IdentidadeJuridicaSnapshot> snapshots = new ArrayList<>();
        snapshots.add(seedOnlySnapshot(consulta));
        snapshots.addAll(futures.stream().map(future -> future.getNow(null)).filter(java.util.Objects::nonNull).toList());
        return List.copyOf(snapshots);
    }

    private String sanitizeOperationSegment(String value) {
        if (value == null || value.isBlank()) {
            return "fonte-indefinida";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private void awaitSnapshots(List<CompletableFuture<IdentidadeJuridicaSnapshot>> futures) {
        if (futures == null || futures.isEmpty()) {
            return;
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(FONTE_RESOLUTION_TIMEOUT.plusMillis(500).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            cancelSnapshots(futures);
        } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            cancelSnapshots(futures);
        }
    }

    private void cancelSnapshots(List<CompletableFuture<IdentidadeJuridicaSnapshot>> futures) {
        for (CompletableFuture<IdentidadeJuridicaSnapshot> future : futures) {
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    private IdentidadeJuridicaSnapshot timeoutSnapshot(IdentidadeJuridicaFontePort fonte) {
        return new IdentidadeJuridicaSnapshot(
                fonte.codigoFonte(),
                true,
                List.of(),
                List.of(),
                List.of("Fonte degradada por timeout controlado na resolução do grafo de identidade."),
                "TIMEOUT_FONTE"
        );
    }

    private IdentidadeJuridicaSnapshot failureSnapshot(IdentidadeJuridicaFontePort fonte, Throwable ex) {
        return new IdentidadeJuridicaSnapshot(
                fonte.codigoFonte(),
                true,
                List.of(),
                List.of(),
                List.of("Fonte degradada durante a resolução assíncrona do grafo de identidade."),
                ex == null ? "ASYNC_FAILURE" : ex.getClass().getSimpleName() + ":" + blankToNull(ex.getMessage())
        );
    }

    private IdentidadeJuridicaSnapshot resolverFonte(IdentidadeJuridicaFontePort fonte, IdentidadeJuridicaConsulta consulta) {
        try {
            IdentidadeJuridicaSnapshot snapshot = fonte.resolver(consulta);
            return snapshot == null
                    ? new IdentidadeJuridicaSnapshot(fonte.codigoFonte(), true, List.of(), List.of(), List.of(), "FONTE_RETORNOU_NULO")
                    : snapshot;
        } catch (RuntimeException ex) {
            return new IdentidadeJuridicaSnapshot(
                    fonte.codigoFonte(),
                    true,
                    List.of(),
                    List.of(),
                    List.of("Fonte degradada durante a resolução do grafo de identidade."),
                    ex.getClass().getSimpleName() + ":" + blankToNull(ex.getMessage())
            );
        }
    }

    private IdentidadeJuridicaSnapshot seedOnlySnapshot(IdentidadeJuridicaConsulta consulta) {
        ArrayList<IdentidadeJuridicaVertice> vertices = new ArrayList<>();
        for (IdentidadeJuridicaSemente seed : consulta.sementes()) {
            vertices.add(new IdentidadeJuridicaVertice(
                    vertexId(tipoSementeParaVertice(seed.tipo()), seed.valor()),
                    tipoSementeParaVertice(seed.tipo()),
                    seed.valor(),
                    blankToNull(seed.rotulo()) == null ? seed.valor() : seed.rotulo(),
                    1d,
                    Set.of("SEMENTE"),
                    seed.atributos()
            ));
        }
        for (String numeroProcesso : consulta.processosRaiz()) {
            vertices.add(new IdentidadeJuridicaVertice(
                    vertexId(IdentidadeJuridicaVerticeTipo.PROCESSO, numeroProcesso),
                    IdentidadeJuridicaVerticeTipo.PROCESSO,
                    numeroProcesso,
                    numeroProcesso,
                    1d,
                    Set.of("SEMENTE"),
                    Map.of("classe", "PROCESSO_RAIZ")
            ));
        }
        return new IdentidadeJuridicaSnapshot(
                "SEMENTE",
                false,
                vertices,
                List.of(),
                List.of("A consulta foi inicializada com sementes normalizadas e pronta para ser enriquecida por conectores internos e externos."),
                null
        );
    }

    private GraphProjection projetar(IdentidadeJuridicaConsulta consulta, List<IdentidadeJuridicaSnapshot> snapshots) {
        LinkedHashMap<String, IdentidadeJuridicaVerticeAccumulator> vertices = new LinkedHashMap<>();
        LinkedHashMap<String, IdentidadeJuridicaArestaAccumulator> arestas = new LinkedHashMap<>();
        LinkedHashSet<String> seedVertexIds = new LinkedHashSet<>();

        for (IdentidadeJuridicaSemente seed : consulta.sementes()) {
            IdentidadeJuridicaVerticeTipo tipo = tipoSementeParaVertice(seed.tipo());
            String canonical = canonicalize(seed.tipo(), seed.valor());
            String id = vertexId(tipo, canonical);
            seedVertexIds.add(id);
            mergeVertice(vertices, new IdentidadeJuridicaVertice(
                    id,
                    tipo,
                    canonical,
                    blankToNull(seed.rotulo()) == null ? canonical : seed.rotulo(),
                    1d,
                    Set.of("SEMENTE"),
                    seed.atributos()
            ));
        }
        for (String numeroProcesso : consulta.processosRaiz()) {
            String normalized = normalizeProcessNumber(numeroProcesso);
            String id = vertexId(IdentidadeJuridicaVerticeTipo.PROCESSO, normalized);
            seedVertexIds.add(id);
            mergeVertice(vertices, new IdentidadeJuridicaVertice(
                    id,
                    IdentidadeJuridicaVerticeTipo.PROCESSO,
                    normalized,
                    normalized,
                    1d,
                    Set.of("SEMENTE"),
                    Map.of("classe", "PROCESSO_RAIZ")
            ));
        }

        Map<String, String> aliases = new HashMap<>();
        for (IdentidadeJuridicaSnapshot snapshot : snapshots) {
            for (IdentidadeJuridicaVertice rawVertice : snapshot.vertices()) {
                IdentidadeJuridicaVertice normalized = normalizeVertice(rawVertice, snapshot.fonteCodigo());
                aliases.put(rawVertice.id(), normalized.id());
                if (vertices.size() >= consulta.limiteVertices() && !vertices.containsKey(normalized.id())) {
                    continue;
                }
                mergeVertice(vertices, normalized);
            }
        }

        for (IdentidadeJuridicaSnapshot snapshot : snapshots) {
            for (IdentidadeJuridicaAresta rawAresta : snapshot.arestas()) {
                String origemId = aliases.getOrDefault(rawAresta.origemId(), rawAresta.origemId());
                String destinoId = aliases.getOrDefault(rawAresta.destinoId(), rawAresta.destinoId());
                IdentidadeJuridicaAresta adjusted = new IdentidadeJuridicaAresta(
                        rawAresta.id(),
                        origemId,
                        destinoId,
                        rawAresta.tipo(),
                        rawAresta.confianca(),
                        rawAresta.bidirecional(),
                        rawAresta.fundamentos(),
                        rawAresta.atributos()
                );
                IdentidadeJuridicaAresta aresta = normalizeAresta(adjusted, snapshot.fonteCodigo());
                if (arestas.size() >= consulta.limiteArestas() && !arestas.containsKey(aresta.id())) {
                    continue;
                }
                if (!vertices.containsKey(aresta.origemId()) || !vertices.containsKey(aresta.destinoId())) {
                    continue;
                }
                mergeAresta(arestas, aresta);
            }
        }

        List<IdentidadeJuridicaVertice> mergedVertices = vertices.values().stream().map(IdentidadeJuridicaVerticeAccumulator::toVertice).toList();
        List<IdentidadeJuridicaAresta> mergedArestas = arestas.values().stream().map(IdentidadeJuridicaArestaAccumulator::toAresta).toList();
        Map<String, List<IdentidadeJuridicaAresta>> adjacency = buildAdjacency(mergedArestas);
        return new GraphProjection(mergedVertices, mergedArestas, adjacency, List.copyOf(seedVertexIds));
    }

    private IdentidadeJuridicaVertice normalizeVertice(IdentidadeJuridicaVertice vertice, String fonteCodigo) {
        String canonical = normalizeCanonicalForVertexType(vertice.tipo(), vertice.chaveCanonica());
        return new IdentidadeJuridicaVertice(
                vertexId(vertice.tipo(), canonical),
                vertice.tipo(),
                canonical,
                blankToNull(vertice.rotulo()) == null ? canonical : vertice.rotulo(),
                vertice.confianca(),
                mergeSet(vertice.fontes(), Set.of(fonteCodigo)),
                vertice.atributos()
        );
    }

    private IdentidadeJuridicaAresta normalizeAresta(IdentidadeJuridicaAresta aresta, String fonteCodigo) {
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(aresta.fundamentos());
        fundamentos.add("Fonte=" + fonteCodigo);
        String canonicalId = edgeId(aresta.origemId(), aresta.destinoId(), aresta.tipo(), aresta.bidirecional(), aresta.atributos());
        return new IdentidadeJuridicaAresta(
                canonicalId,
                aresta.origemId(),
                aresta.destinoId(),
                aresta.tipo(),
                aresta.confianca(),
                aresta.bidirecional(),
                fundamentos,
                aresta.atributos()
        );
    }

    private void mergeVertice(Map<String, IdentidadeJuridicaVerticeAccumulator> target, IdentidadeJuridicaVertice vertice) {
        target.compute(vertice.id(), (id, current) -> current == null
                ? new IdentidadeJuridicaVerticeAccumulator(vertice)
                : current.merge(vertice));
    }

    private void mergeAresta(Map<String, IdentidadeJuridicaArestaAccumulator> target, IdentidadeJuridicaAresta aresta) {
        target.compute(aresta.id(), (id, current) -> current == null
                ? new IdentidadeJuridicaArestaAccumulator(aresta)
                : current.merge(aresta));
    }

    private List<IdentidadeJuridicaCaminho> detectarConexoesOcultas(IdentidadeJuridicaConsulta consulta, GraphProjection projection) {
        ArrayList<IdentidadeJuridicaCaminho> caminhos = new ArrayList<>();
        List<String> seeds = projection.seedVertexIds();
        for (int i = 0; i < seeds.size(); i++) {
            for (int j = i + 1; j < seeds.size(); j++) {
                Optional<PathCandidate> candidate = shortestPath(seeds.get(i), seeds.get(j), consulta.profundidadeMaxima(), projection.adjacency());
                candidate.filter(this::isOculta).ifPresent(path -> caminhos.add(toCaminho(path, projection)));
            }
        }
        return caminhos.stream()
                .sorted(Comparator.comparing(IdentidadeJuridicaCaminho::confianca).reversed().thenComparing(IdentidadeJuridicaCaminho::codigo))
                .toList();
    }

    private List<IdentidadeJuridicaAchado> detectarAchados(IdentidadeJuridicaConsulta consulta,
                                                            GraphProjection projection,
                                                            List<IdentidadeJuridicaSnapshot> snapshots,
                                                            List<IdentidadeJuridicaCaminho> conexoesOcultas) {
        ArrayList<IdentidadeJuridicaAchado> achados = new ArrayList<>();
        achados.addAll(detectarFontesDegradadas(snapshots));
        achados.addAll(detectarConexoesOcultasAchados(conexoesOcultas));
        achados.addAll(detectarLitiganciaRepetitiva(projection));
        achados.addAll(detectarGruposEconomicos(projection));
        achados.addAll(detectarConflitosDeInteresse(projection));
        achados.addAll(detectarFraudeRepresentacao(projection));
        achados.addAll(detectarExecucoesCruzadas(projection));
        achados.addAll(detectarDensidadeAnomala(consulta, projection));
        return achados.stream()
                .sorted(Comparator.comparing(IdentidadeJuridicaAchado::risco).reversed().thenComparing(IdentidadeJuridicaAchado::codigo))
                .toList();
    }

    private List<IdentidadeJuridicaAchado> detectarFontesDegradadas(List<IdentidadeJuridicaSnapshot> snapshots) {
        return snapshots.stream()
                .filter(IdentidadeJuridicaSnapshot::degradada)
                .map(snapshot -> new IdentidadeJuridicaAchado(
                        code("fonte", snapshot.fonteCodigo()),
                        IdentidadeJuridicaAchadoTipo.FONTE_DEGRADADA,
                        IdentidadeJuridicaRiscoNivel.MEDIO,
                        "Fonte degradada",
                        "A resolução da fonte " + snapshot.fonteCodigo() + " falhou ou devolveu um quadro incompleto durante a montagem do grafo.",
                        List.of(),
                        List.of(),
                        List.of("intake", "connectors", "observability", "governance"),
                        snapshot.diagnostico() == null ? List.of("A fonte deve ser reprocessada ou auditada antes de decisões distributivas automatizadas.") : List.of(snapshot.diagnostico())
                ))
                .toList();
    }

    private List<IdentidadeJuridicaAchado> detectarConexoesOcultasAchados(List<IdentidadeJuridicaCaminho> conexoesOcultas) {
        return conexoesOcultas.stream()
                .map(caminho -> new IdentidadeJuridicaAchado(
                        caminho.codigo(),
                        IdentidadeJuridicaAchadoTipo.CONEXAO_OCULTA,
                        riscoPorConfianca(caminho.confianca()),
                        "Conexão oculta entre polos ou entidades",
                        caminho.explicacao(),
                        caminho.verticesIds(),
                        caminho.arestasIds(),
                        List.of("core/processo/prevencao", "core/processo/conexao", "core/processo/distribuicao", "ai/jurimetria"),
                        caminho.fundamentos()
                ))
                .toList();
    }

    private List<IdentidadeJuridicaAchado> detectarLitiganciaRepetitiva(GraphProjection projection) {
        Map<String, List<IdentidadeJuridicaAresta>> adjacency = projection.adjacency();
        Map<String, IdentidadeJuridicaVertice> byId = projection.vertices().stream().collect(Collectors.toMap(IdentidadeJuridicaVertice::id, value -> value));
        ArrayList<IdentidadeJuridicaAchado> achados = new ArrayList<>();
        for (IdentidadeJuridicaVertice vertice : projection.vertices()) {
            if (vertice.tipo() == IdentidadeJuridicaVerticeTipo.PROCESSO || vertice.tipo() == IdentidadeJuridicaVerticeTipo.DOCUMENTO) {
                continue;
            }
            LinkedHashSet<String> processos = new LinkedHashSet<>();
            LinkedHashSet<String> arestas = new LinkedHashSet<>();
            for (IdentidadeJuridicaAresta aresta : adjacency.getOrDefault(vertice.id(), List.of())) {
                if (!relacaoProcessual(aresta.tipo())) {
                    continue;
                }
                String other = otherEndpoint(aresta, vertice.id());
                IdentidadeJuridicaVertice destino = byId.get(other);
                if (destino != null && destino.tipo() == IdentidadeJuridicaVerticeTipo.PROCESSO) {
                    processos.add(destino.id());
                    arestas.add(aresta.id());
                }
            }
            if (processos.size() >= 4) {
                achados.add(new IdentidadeJuridicaAchado(
                        code("litigante", vertice.id()),
                        IdentidadeJuridicaAchadoTipo.LITIGANCIA_REPETITIVA,
                        processos.size() >= 10 ? IdentidadeJuridicaRiscoNivel.CRITICO : IdentidadeJuridicaRiscoNivel.ALTO,
                        "Litigância repetitiva ou presença massiva",
                        vertice.rotulo() + " aparece conectado a " + processos.size() + " processos no recorte atual do grafo.",
                        mergeList(List.of(vertice.id()), processos),
                        List.copyOf(arestas),
                        List.of("core/processo/unificado", "core/processo/prevencao", "core/processo/conexao", "ai/jurimetria"),
                        List.of(
                                "O motor consolidou recorrência processual por identidade canônica.",
                                "O volume repetitivo pode alterar prevenção, conexão, dependência e políticas de fila ou mutirão."
                        )
                ));
            }
        }
        return achados;
    }

    private List<IdentidadeJuridicaAchado> detectarGruposEconomicos(GraphProjection projection) {
        Map<String, IdentidadeJuridicaVertice> byId = projection.vertices().stream().collect(Collectors.toMap(IdentidadeJuridicaVertice::id, value -> value));
        Map<String, List<IdentidadeJuridicaAresta>> adjacency = projection.adjacency();
        ArrayList<IdentidadeJuridicaAchado> achados = new ArrayList<>();
        for (IdentidadeJuridicaVertice hub : projection.vertices()) {
            if (!hubEstrutural(hub.tipo())) {
                continue;
            }
            LinkedHashSet<String> empresas = new LinkedHashSet<>();
            LinkedHashSet<String> arestas = new LinkedHashSet<>();
            for (IdentidadeJuridicaAresta aresta : adjacency.getOrDefault(hub.id(), List.of())) {
                if (!vinculoGrupoEconomico(aresta.tipo())) {
                    continue;
                }
                String other = otherEndpoint(aresta, hub.id());
                IdentidadeJuridicaVertice candidato = byId.get(other);
                if (candidato != null && candidato.tipo() == IdentidadeJuridicaVerticeTipo.PESSOA_JURIDICA) {
                    empresas.add(candidato.id());
                    arestas.add(aresta.id());
                }
            }
            if (empresas.size() >= 2) {
                achados.add(new IdentidadeJuridicaAchado(
                        code("grupo-economico", hub.id()),
                        IdentidadeJuridicaAchadoTipo.GRUPO_ECONOMICO_PROVAVEL,
                        empresas.size() >= 4 ? IdentidadeJuridicaRiscoNivel.CRITICO : IdentidadeJuridicaRiscoNivel.ALTO,
                        "Grupo econômico provável",
                        "O hub " + hub.rotulo() + " conecta " + empresas.size() + " pessoas jurídicas com materialidade suficiente para prevenção, reunião ou execução coordenada.",
                        mergeList(List.of(hub.id()), empresas),
                        List.copyOf(arestas),
                        List.of("core/identidade/vinculo", "core/processo/prevencao", "core/processo/conexao", "core/processo/dependencia"),
                        List.of(
                                "Sócios, representantes, endereços, domínios e canais de contato compartilham peso indiciário no grafo.",
                                "A classificação não substitui decisão judicial, mas antecipa agrupamentos economicamente relevantes."
                        )
                ));
            }
        }
        return achados;
    }

    private List<IdentidadeJuridicaAchado> detectarConflitosDeInteresse(GraphProjection projection) {
        ArrayList<IdentidadeJuridicaAchado> achados = new ArrayList<>();
        Map<String, List<IdentidadeJuridicaAresta>> adjacency = projection.adjacency();
        for (IdentidadeJuridicaVertice representante : projection.vertices()) {
            if (!representative(representante.tipo())) {
                continue;
            }
            LinkedHashSet<String> polos = new LinkedHashSet<>();
            LinkedHashSet<String> vertices = new LinkedHashSet<>();
            LinkedHashSet<String> arestas = new LinkedHashSet<>();
            for (IdentidadeJuridicaAresta aresta : adjacency.getOrDefault(representante.id(), List.of())) {
                if (aresta.tipo() != IdentidadeJuridicaArestaTipo.REPRESENTA && aresta.tipo() != IdentidadeJuridicaArestaTipo.PROCURA_POR) {
                    continue;
                }
                String polo = blankToNull(aresta.atributos().get("polo"));
                if (polo != null) {
                    polos.add(polo.toUpperCase(Locale.ROOT));
                }
                vertices.add(otherEndpoint(aresta, representante.id()));
                arestas.add(aresta.id());
            }
            if (polos.contains("ATIVO") && polos.contains("PASSIVO")) {
                achados.add(new IdentidadeJuridicaAchado(
                        code("conflito", representante.id()),
                        IdentidadeJuridicaAchadoTipo.CONFLITO_INTERESSE_POTENCIAL,
                        IdentidadeJuridicaRiscoNivel.CRITICO,
                        "Conflito de interesse potencial",
                        representante.rotulo() + " aparece vinculado a polos antagônicos no mesmo recorte do grafo.",
                        mergeList(List.of(representante.id()), vertices),
                        List.copyOf(arestas),
                        List.of("core/processo/prevencao", "core/processo/conexao", "core/governance", "core/explainability"),
                        List.of(
                                "O conflito foi inferido por polos declarados em arestas de representação.",
                                "A trilha deve ser submetida à governança, ao magistrado e ao controle de impedimentos."
                        )
                ));
            }
        }
        return achados;
    }

    private List<IdentidadeJuridicaAchado> detectarFraudeRepresentacao(GraphProjection projection) {
        ArrayList<IdentidadeJuridicaAchado> achados = new ArrayList<>();
        Map<String, List<IdentidadeJuridicaAresta>> adjacency = projection.adjacency();
        for (IdentidadeJuridicaVertice representante : projection.vertices()) {
            if (!representative(representante.tipo())) {
                continue;
            }
            LinkedHashSet<String> representados = new LinkedHashSet<>();
            LinkedHashSet<String> invalidas = new LinkedHashSet<>();
            for (IdentidadeJuridicaAresta aresta : adjacency.getOrDefault(representante.id(), List.of())) {
                if (aresta.tipo() != IdentidadeJuridicaArestaTipo.REPRESENTA && aresta.tipo() != IdentidadeJuridicaArestaTipo.PROCURA_POR) {
                    continue;
                }
                representados.add(otherEndpoint(aresta, representante.id()));
                String status = blankToNull(aresta.atributos().get("statusRepresentacao"));
                if (status != null && Set.of("REVOGADA", "EXPIRADA", "SUSPENSA", "INVALIDA").contains(status.toUpperCase(Locale.ROOT))) {
                    invalidas.add(aresta.id());
                }
            }
            if (!invalidas.isEmpty() || representados.size() >= 15) {
                achados.add(new IdentidadeJuridicaAchado(
                        code("fraude-representacao", representante.id()),
                        IdentidadeJuridicaAchadoTipo.FRAUDE_REPRESENTACAO_POTENCIAL,
                        !invalidas.isEmpty() ? IdentidadeJuridicaRiscoNivel.CRITICO : IdentidadeJuridicaRiscoNivel.ALTO,
                        "Fraude ou anomalia de representação potencial",
                        representante.rotulo() + " atingiu um padrão de representação que exige saneamento documental e validação cruzada.",
                        mergeList(List.of(representante.id()), representados),
                        new ArrayList<>(invalidas),
                        List.of("core/identidade/resolucao", "core/processo/parte", "core/governance", "core/security"),
                        invalidas.isEmpty()
                                ? List.of("Um mesmo representante assumiu amplitude anômala de representação neste recorte.")
                                : List.of("Há representação revogada, expirada, suspensa ou inválida conectada ao mesmo agente.")
                ));
            }
        }
        return achados;
    }

    private List<IdentidadeJuridicaAchado> detectarExecucoesCruzadas(GraphProjection projection) {
        ArrayList<IdentidadeJuridicaAchado> achados = new ArrayList<>();
        Map<String, List<IdentidadeJuridicaAresta>> adjacency = projection.adjacency();
        Map<String, IdentidadeJuridicaVertice> byId = projection.vertices().stream().collect(Collectors.toMap(IdentidadeJuridicaVertice::id, value -> value));
        for (IdentidadeJuridicaVertice vertice : projection.vertices()) {
            if (vertice.tipo() != IdentidadeJuridicaVerticeTipo.EXECUCAO && vertice.tipo() != IdentidadeJuridicaVerticeTipo.PROCESSO) {
                continue;
            }
            LinkedHashSet<String> correlatos = new LinkedHashSet<>();
            LinkedHashSet<String> arestas = new LinkedHashSet<>();
            for (IdentidadeJuridicaAresta aresta : adjacency.getOrDefault(vertice.id(), List.of())) {
                if (aresta.tipo() != IdentidadeJuridicaArestaTipo.EXECUCAO_CRUZADA && aresta.tipo() != IdentidadeJuridicaArestaTipo.DEPENDE_DE) {
                    continue;
                }
                String other = otherEndpoint(aresta, vertice.id());
                IdentidadeJuridicaVertice target = byId.get(other);
                if (target != null && (target.tipo() == IdentidadeJuridicaVerticeTipo.EXECUCAO || target.tipo() == IdentidadeJuridicaVerticeTipo.PROCESSO)) {
                    correlatos.add(other);
                    arestas.add(aresta.id());
                }
            }
            if (correlatos.size() >= 2) {
                achados.add(new IdentidadeJuridicaAchado(
                        code("execucao-cruzada", vertice.id()),
                        IdentidadeJuridicaAchadoTipo.EXECUCAO_CRUZADA_RELEVANTE,
                        IdentidadeJuridicaRiscoNivel.ALTO,
                        "Execução cruzada ou dependência relevante",
                        vertice.rotulo() + " possui dependências e execuções cruzadas suficientes para travamento coordenado de atos decisórios e constritivos.",
                        mergeList(List.of(vertice.id()), correlatos),
                        List.copyOf(arestas),
                        List.of("core/processo/dependencia", "core/processo/execucao", "core/processo/conexao"),
                        List.of("A malha de identidade encontrou execuções e dependências recíprocas no mesmo cluster processual.")
                ));
            }
        }
        return achados;
    }

    private List<IdentidadeJuridicaAchado> detectarDensidadeAnomala(IdentidadeJuridicaConsulta consulta, GraphProjection projection) {
        double densidade = density(projection.vertices().size(), projection.arestas().size());
        if (densidade < 0.18d || projection.vertices().size() < 8) {
            return List.of();
        }
        IdentidadeJuridicaRiscoNivel risco = densidade >= 0.40d || projection.arestas().size() > consulta.limiteArestas() * 0.65d
                ? IdentidadeJuridicaRiscoNivel.ALTO
                : IdentidadeJuridicaRiscoNivel.MEDIO;
        return List.of(new IdentidadeJuridicaAchado(
                code("densidade", consulta.correlacaoId()),
                IdentidadeJuridicaAchadoTipo.DENSIDADE_ANOMALA,
                risco,
                "Densidade relacional anômala",
                "O recorte consultado atingiu densidade relacional de " + format(densidade) + ", valor suficiente para triagem prioritária em prevenção, conexão e fraude.",
                projection.seedVertexIds(),
                List.of(),
                List.of("core/processo/prevencao", "core/processo/conexao", "core/governance", "observability"),
                List.of(
                        "A densidade cresce quando múltiplas entidades compartilham vínculos materialmente fortes.",
                        "Clusters densos exigem revisão de prevenção, dependência, sigilo e governança de acesso."
                )
        ));
    }

    private IdentidadeJuridicaResumo resumir(IdentidadeJuridicaConsulta consulta,
                                             List<IdentidadeJuridicaSnapshot> snapshots,
                                             GraphProjection projection,
                                             List<IdentidadeJuridicaCaminho> conexoesOcultas,
                                             List<IdentidadeJuridicaAchado> achados,
                                             String fingerprint) {
        long litigantes = achados.stream().filter(item -> item.tipo() == IdentidadeJuridicaAchadoTipo.LITIGANCIA_REPETITIVA).count();
        long grupos = achados.stream().filter(item -> item.tipo() == IdentidadeJuridicaAchadoTipo.GRUPO_ECONOMICO_PROVAVEL).count();
        long conflitos = achados.stream().filter(item -> item.tipo() == IdentidadeJuridicaAchadoTipo.CONFLITO_INTERESSE_POTENCIAL).count();
        long fraudesRepresentacao = achados.stream().filter(item -> item.tipo() == IdentidadeJuridicaAchadoTipo.FRAUDE_REPRESENTACAO_POTENCIAL).count();
        int totalProcessosCorrelatos = (int) consulta.processosRaiz().size();
        IdentidadeJuridicaResumo.RiscoGlobal riscoGlobal = resolveRiscoGlobal(litigantes, grupos, conflitos, fraudesRepresentacao, conexoesOcultas.size());
        return new IdentidadeJuridicaResumo(
                consulta.sementes().size() + consulta.processosRaiz().size(),
                snapshots.size(),
                (int) snapshots.stream().filter(IdentidadeJuridicaSnapshot::degradada).count(),
                projection.vertices().size(),
                projection.arestas().size(),
                conexoesOcultas.size(),
                achados.size(),
                litigantes,
                grupos,
                conflitos,
                totalProcessosCorrelatos,
                riscoGlobal,
                litigantes > 0,
                conflitos > 0,
                fraudesRepresentacao > 0,
                density(projection.vertices().size(), projection.arestas().size()),
                fingerprint
        );
    }

    private IdentidadeJuridicaResumo.RiscoGlobal resolveRiscoGlobal(long litigantes,
                                                                   long grupos,
                                                                   long conflitos,
                                                                   long fraudesRepresentacao,
                                                                   int conexoesOcultas) {
        long score = litigantes * 2L + grupos * 2L + conflitos * 3L + fraudesRepresentacao * 4L + conexoesOcultas;
        if (score >= 12L) {
            return IdentidadeJuridicaResumo.RiscoGlobal.CRITICO;
        }
        if (score >= 7L) {
            return IdentidadeJuridicaResumo.RiscoGlobal.ALTO;
        }
        if (score >= 3L) {
            return IdentidadeJuridicaResumo.RiscoGlobal.MODERADO;
        }
        return IdentidadeJuridicaResumo.RiscoGlobal.BAIXO;
    }

    private List<String> consolidarFundamentos(IdentidadeJuridicaConsulta consulta,
                                               List<IdentidadeJuridicaSnapshot> snapshots,
                                               List<IdentidadeJuridicaAchado> achados,
                                               List<IdentidadeJuridicaCaminho> conexoesOcultas,
                                               IdentidadeJuridicaResumo resumo) {
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("A fundação do grafo jurídico consolida identidade, representação, recorrência processual e dependências operacionais em um único recorte correlacionado.");
        fundamentos.add("Os módulos imediatamente impactados são: " + String.join(", ", MODULOS_BASE) + ".");
        fundamentos.add("O recorte atual partiu de " + resumo.totalSementes() + " sementes e consultou " + resumo.totalFontesConsultadas() + " fontes com " + resumo.totalFontesDegradadas() + " degradações observadas.");
        if (!consulta.processosRaiz().isEmpty()) {
            fundamentos.add("Processos raiz influenciando prevenção, conexão e dependência: " + String.join(", ", consulta.processosRaiz()) + ".");
        }
        snapshots.stream().flatMap(snapshot -> snapshot.fundamentos().stream()).forEach(fundamentos::add);
        conexoesOcultas.stream().flatMap(caminho -> caminho.fundamentos().stream()).limit(8).forEach(fundamentos::add);
        achados.stream().flatMap(achado -> achado.fundamentos().stream()).limit(20).forEach(fundamentos::add);
        fundamentos.add("Fingerprint criptográfico do recorte: " + resumo.fingerprint());
        return List.copyOf(fundamentos);
    }

    private IdentidadeJuridicaPersistencia persistir(IdentidadeJuridicaConsulta consulta, IdentidadeJuridicaGraphAggregate aggregate) {
        if (!consulta.persistirNoGrafo() || graphStore == null) {
            return null;
        }
        return graphStore.persistir(aggregate);
    }

    private void registrarExplainability(IdentidadeJuridicaGraphAggregate aggregate,
                                         IdentidadeJuridicaConsulta consulta,
                                         List<IdentidadeJuridicaSnapshot> snapshots) {
        if (decisionTraceService == null) {
            return;
        }
        decisionTraceService.record(
                "IDENTIDADE_JURIDICA_GRAFO",
                "IDENTIDADE_GRAFO",
                aggregate.correlacaoId(),
                BigDecimal.valueOf(confidence(aggregate)),
                toJson(aggregate.achados().stream().map(IdentidadeJuridicaAchado::codigo).toList()),
                toJson(aggregate.fundamentos()),
                Hashes.sha256Hex(toJson(consulta)),
                Hashes.sha256Hex(aggregate.resumo().fingerprint()),
                "pjb-identidade-grafo-v1",
                toJson(Map.of(
                        "fontes", snapshots.stream().map(IdentidadeJuridicaSnapshot::fonteCodigo).toList(),
                        "fontesDegradadas", snapshots.stream().filter(IdentidadeJuridicaSnapshot::degradada).map(IdentidadeJuridicaSnapshot::fonteCodigo).toList(),
                        "achados", aggregate.achados().stream().map(IdentidadeJuridicaAchado::tipo).map(Enum::name).toList(),
                        "densidade", aggregate.resumo().densidade(),
                        "tempoProcessamentoMs", aggregate.tempoProcessamento().toMillis()
                ))
        );
    }

    private void publicarEvento(IdentidadeJuridicaConsulta consulta, IdentidadeJuridicaGraphAggregate aggregate) {
        if (!consulta.publicarEvento() || domainEventPublisher == null) {
            return;
        }
        domainEventPublisher.publish(new IdentidadeJuridicaGraphAtualizadoEvent(
                aggregate.correlacaoId(),
                aggregate.resumo().fingerprint(),
                aggregate.achados().stream().map(IdentidadeJuridicaAchado::codigo).toList(),
                aggregate.geradoEm()
        ));
    }

    private void registrarMetricas(IdentidadeJuridicaGraphAggregate aggregate) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("pjb.identidade.grafo.execucoes").increment();
        meterRegistry.summary("pjb.identidade.grafo.vertices").record(aggregate.vertices().size());
        meterRegistry.summary("pjb.identidade.grafo.arestas").record(aggregate.arestas().size());
        meterRegistry.summary("pjb.identidade.grafo.achados").record(aggregate.achados().size());
        meterRegistry.summary("pjb.identidade.grafo.densidade").record(aggregate.resumo().densidade());
    }

    private Optional<PathCandidate> shortestPath(String source,
                                                 String target,
                                                 int maxDepth,
                                                 Map<String, List<IdentidadeJuridicaAresta>> adjacency) {
        ArrayDeque<PathCandidate> queue = new ArrayDeque<>();
        queue.add(new PathCandidate(List.of(source), List.of(), 1d));
        while (!queue.isEmpty()) {
            PathCandidate current = queue.removeFirst();
            String head = current.lastVertex();
            if (head.equals(target) && current.vertices().size() > 1) {
                return Optional.of(current);
            }
            if (current.depth() >= maxDepth) {
                continue;
            }
            for (IdentidadeJuridicaAresta edge : adjacency.getOrDefault(head, List.of())) {
                String next = otherEndpoint(edge, head);
                if (current.contains(next)) {
                    continue;
                }
                queue.addLast(current.advance(next, edge));
            }
        }
        return Optional.empty();
    }

    private boolean isOculta(PathCandidate path) {
        return path.vertices().size() >= 3 && path.edges().stream().map(IdentidadeJuridicaAresta::tipo).distinct().count() >= 2;
    }

    private IdentidadeJuridicaCaminho toCaminho(PathCandidate candidate, GraphProjection projection) {
        String codigo = code("conexao-oculta", String.join("|", candidate.vertices()));
        List<String> fundamentos = candidate.edges().stream()
                .flatMap(edge -> edge.fundamentos().stream())
                .distinct()
                .limit(8)
                .toList();
        String explicacao = "O grafo encontrou um caminho latente entre sementes através de "
                + candidate.vertices().size()
                + " vértices e "
                + candidate.edges().size()
                + " arestas, suficiente para acionar análise de prevenção, conexão e recorrência.";
        return new IdentidadeJuridicaCaminho(
                codigo,
                candidate.vertices(),
                candidate.edges().stream().map(IdentidadeJuridicaAresta::id).toList(),
                candidate.confidence(),
                explicacao,
                fundamentos.isEmpty() ? List.of("O caminho foi consolidado por múltiplos vínculos independentes no grafo.") : fundamentos
        );
    }

    private Map<String, List<IdentidadeJuridicaAresta>> buildAdjacency(List<IdentidadeJuridicaAresta> arestas) {
        HashMap<String, ArrayList<IdentidadeJuridicaAresta>> adjacency = new HashMap<>();
        for (IdentidadeJuridicaAresta edge : arestas) {
            adjacency.computeIfAbsent(edge.origemId(), ignored -> new ArrayList<>()).add(edge);
            if (edge.bidirecional()) {
                adjacency.computeIfAbsent(edge.destinoId(), ignored -> new ArrayList<>()).add(edge);
            } else {
                adjacency.computeIfAbsent(edge.destinoId(), ignored -> new ArrayList<>());
            }
        }
        return adjacency.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    private String vertexId(IdentidadeJuridicaVerticeTipo tipo, String canonical) {
        return DeterministicUuid.v5("pjb-identidade-vertice", tipo.name() + ":" + canonical).toString();
    }

    private String edgeId(String origemId,
                          String destinoId,
                          IdentidadeJuridicaArestaTipo tipo,
                          boolean bidirecional,
                          Map<String, String> atributos) {
        String left = origemId;
        String right = destinoId;
        if (bidirecional && left.compareTo(right) > 0) {
            left = destinoId;
            right = origemId;
        }
        String signature = left + "|" + tipo.name() + "|" + right + "|" + toJson(atributos);
        return DeterministicUuid.v5("pjb-identidade-aresta", signature).toString();
    }

    private String fingerprint(List<IdentidadeJuridicaVertice> vertices,
                               List<IdentidadeJuridicaAresta> arestas,
                               List<IdentidadeJuridicaAchado> achados,
                               List<IdentidadeJuridicaCaminho> caminhos) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("vertices", vertices.stream().map(item -> item.id() + "|" + item.tipo() + "|" + item.chaveCanonica()).sorted().toList());
        payload.put("arestas", arestas.stream().map(item -> item.id() + "|" + item.tipo() + "|" + item.origemId() + "|" + item.destinoId()).sorted().toList());
        payload.put("achados", achados.stream().map(item -> item.codigo() + "|" + item.tipo()).sorted().toList());
        payload.put("caminhos", caminhos.stream().map(item -> item.codigo()).sorted().toList());
        return Hashes.sha256Hex(toJson(payload));
    }

    private String canonicalize(IdentidadeJuridicaChaveTipo tipo, String value) {
        String normalized = Objects.toString(value, "").trim();
        return switch (tipo) {
            case CPF, CNPJ, TELEFONE, OAB, DOCUMENTO, PROCESSO -> normalized.replaceAll("\\D+", "");
            case EMAIL, DOMINIO -> normalized.toLowerCase(Locale.ROOT);
            case NOME, ORGAO_PUBLICO, UNIDADE, ENDERECO, UUID_EXTERNO -> normalized.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        };
    }

    private String normalizeCanonicalForVertexType(IdentidadeJuridicaVerticeTipo tipo, String value) {
        return switch (tipo) {
            case PESSOA_FISICA -> canonicalize(IdentidadeJuridicaChaveTipo.CPF, value);
            case PESSOA_JURIDICA -> canonicalize(IdentidadeJuridicaChaveTipo.CNPJ, value);
            case ADVOGADO -> canonicalize(IdentidadeJuridicaChaveTipo.OAB, value);
            case ORGAO_PUBLICO -> canonicalize(IdentidadeJuridicaChaveTipo.ORGAO_PUBLICO, value);
            case UNIDADE_JUDICIARIA -> canonicalize(IdentidadeJuridicaChaveTipo.UNIDADE, value);
            case PROCESSO, EXECUCAO, INCIDENTE -> canonicalize(IdentidadeJuridicaChaveTipo.PROCESSO, value);
            case TELEFONE -> canonicalize(IdentidadeJuridicaChaveTipo.TELEFONE, value);
            case EMAIL -> canonicalize(IdentidadeJuridicaChaveTipo.EMAIL, value);
            case ENDERECO -> canonicalize(IdentidadeJuridicaChaveTipo.ENDERECO, value);
            case DOMINIO -> canonicalize(IdentidadeJuridicaChaveTipo.DOMINIO, value);
            case DOCUMENTO -> canonicalize(IdentidadeJuridicaChaveTipo.DOCUMENTO, value);
            default -> Objects.toString(value, "").trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        };
    }

    private IdentidadeJuridicaVerticeTipo tipoSementeParaVertice(IdentidadeJuridicaChaveTipo tipo) {
        return switch (tipo) {
            case CPF, NOME -> IdentidadeJuridicaVerticeTipo.PESSOA_FISICA;
            case CNPJ -> IdentidadeJuridicaVerticeTipo.PESSOA_JURIDICA;
            case OAB -> IdentidadeJuridicaVerticeTipo.ADVOGADO;
            case EMAIL -> IdentidadeJuridicaVerticeTipo.EMAIL;
            case TELEFONE -> IdentidadeJuridicaVerticeTipo.TELEFONE;
            case PROCESSO -> IdentidadeJuridicaVerticeTipo.PROCESSO;
            case ORGAO_PUBLICO -> IdentidadeJuridicaVerticeTipo.ORGAO_PUBLICO;
            case UNIDADE -> IdentidadeJuridicaVerticeTipo.UNIDADE_JUDICIARIA;
            case ENDERECO -> IdentidadeJuridicaVerticeTipo.ENDERECO;
            case DOMINIO -> IdentidadeJuridicaVerticeTipo.DOMINIO;
            case DOCUMENTO -> IdentidadeJuridicaVerticeTipo.DOCUMENTO;
            case UUID_EXTERNO -> IdentidadeJuridicaVerticeTipo.OUTRO;
        };
    }

    private boolean relacaoProcessual(IdentidadeJuridicaArestaTipo tipo) {
        return tipo == IdentidadeJuridicaArestaTipo.PARTE_EM
                || tipo == IdentidadeJuridicaArestaTipo.ATUA_EM
                || tipo == IdentidadeJuridicaArestaTipo.CONECTA_COM
                || tipo == IdentidadeJuridicaArestaTipo.CORRELACIONA_COM;
    }

    private boolean hubEstrutural(IdentidadeJuridicaVerticeTipo tipo) {
        return tipo == IdentidadeJuridicaVerticeTipo.PESSOA_FISICA
                || tipo == IdentidadeJuridicaVerticeTipo.REPRESENTANTE
                || tipo == IdentidadeJuridicaVerticeTipo.ENDERECO
                || tipo == IdentidadeJuridicaVerticeTipo.EMAIL
                || tipo == IdentidadeJuridicaVerticeTipo.TELEFONE
                || tipo == IdentidadeJuridicaVerticeTipo.DOMINIO;
    }

    private boolean vinculoGrupoEconomico(IdentidadeJuridicaArestaTipo tipo) {
        return tipo == IdentidadeJuridicaArestaTipo.SOCIO_DE
                || tipo == IdentidadeJuridicaArestaTipo.PERTENCE_AO_GRUPO
                || tipo == IdentidadeJuridicaArestaTipo.POSSUI_ENDERECO
                || tipo == IdentidadeJuridicaArestaTipo.POSSUI_CONTATO
                || tipo == IdentidadeJuridicaArestaTipo.REPRESENTA;
    }

    private boolean representative(IdentidadeJuridicaVerticeTipo tipo) {
        return tipo == IdentidadeJuridicaVerticeTipo.ADVOGADO
                || tipo == IdentidadeJuridicaVerticeTipo.REPRESENTANTE
                || tipo == IdentidadeJuridicaVerticeTipo.PROCURADORIA;
    }

    private double density(int vertices, int arestas) {
        if (vertices < 2) {
            return 0d;
        }
        double max = vertices * (double) (vertices - 1);
        return arestas / max;
    }

    private double confidence(IdentidadeJuridicaGraphAggregate aggregate) {
        if (aggregate.vertices().isEmpty()) {
            return 0d;
        }
        double vertices = aggregate.vertices().stream().mapToDouble(IdentidadeJuridicaVertice::confianca).average().orElse(0d);
        double arestas = aggregate.arestas().stream().mapToDouble(IdentidadeJuridicaAresta::confianca).average().orElse(0d);
        double degradacao = aggregate.resumo().totalFontesConsultadas() == 0 ? 0d : (double) aggregate.resumo().totalFontesDegradadas() / aggregate.resumo().totalFontesConsultadas();
        return Math.max(0d, Math.min(1d, ((vertices + arestas) / 2d) - (degradacao * 0.15d)));
    }

    private IdentidadeJuridicaRiscoNivel riscoPorConfianca(double confianca) {
        if (confianca >= 0.90d) {
            return IdentidadeJuridicaRiscoNivel.CRITICO;
        }
        if (confianca >= 0.75d) {
            return IdentidadeJuridicaRiscoNivel.ALTO;
        }
        if (confianca >= 0.55d) {
            return IdentidadeJuridicaRiscoNivel.MEDIO;
        }
        return IdentidadeJuridicaRiscoNivel.BAIXO;
    }

    private String otherEndpoint(IdentidadeJuridicaAresta edge, String current) {
        return edge.origemId().equals(current) ? edge.destinoId() : edge.origemId();
    }

    private String normalizeProcessNumber(String value) {
        return canonicalize(IdentidadeJuridicaChaveTipo.PROCESSO, value);
    }

    private Set<String> mergeSet(Set<String> left, Set<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            merged.addAll(right);
        }
        return Set.copyOf(merged);
    }

    private List<String> mergeList(List<String> left, Collection<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            merged.addAll(right);
        }
        return List.copyOf(merged);
    }

    private String code(String prefix, String value) {
        return prefix.toUpperCase(Locale.ROOT) + "_" + Hashes.sha256HexPrefix(Objects.toString(value, ""), 16).toUpperCase(Locale.ROOT);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String blankToNull(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String format(double value) {
        return String.format(Locale.US, "%.4f", value);
    }
}
