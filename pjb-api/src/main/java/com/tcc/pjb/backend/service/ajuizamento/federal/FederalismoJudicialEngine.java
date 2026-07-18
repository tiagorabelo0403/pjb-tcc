package com.tcc.pjb.backend.service.ajuizamento.federal;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoEventoRequest;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoHeartbeatRequest;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoLedgerResponse;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoNodeResponse;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoNodeUpsertRequest;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoOutboxResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.federalismo.ClassificacaoConflitoFederacao;
import com.tcc.pjb.backend.model.entity.federalismo.FederacaoEventoOutbox;
import com.tcc.pjb.backend.model.entity.federalismo.FederacaoLedgerEntry;
import com.tcc.pjb.backend.model.entity.federalismo.NoFederacaoJudicial;
import com.tcc.pjb.backend.model.entity.federalismo.StatusAssinaturaFederacao;
import com.tcc.pjb.backend.model.entity.federalismo.StatusEventoOutboxFederacao;
import com.tcc.pjb.backend.model.entity.federalismo.StatusNoFederacao;
import com.tcc.pjb.backend.model.repository.FederacaoEventoOutboxRepository;
import com.tcc.pjb.backend.model.repository.FederacaoLedgerEntryRepository;
import com.tcc.pjb.backend.model.repository.NoFederacaoJudicialRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class FederalismoJudicialEngine {

    private static final Duration NODE_CACHE_TTL = Duration.ofSeconds(20);

    public static final String TOPIC_FEDERACAO_EVENTOS = "pjb.nacional.federacao.eventos.v1";
    public static final String EVT_FEDERACAO_LEDGER_REGISTRADO = "pjb.federacao.ledger.registrado";
    public static final String EVT_FEDERACAO_OUTBOX_PENDENTE = "pjb.federacao.outbox.pendente";
    public static final String EVT_FEDERACAO_NO_ATUALIZADO = "pjb.federacao.no.atualizado";
    public static final long SCHEMA_VERSION_ATUAL = 1L;

    private final NoFederacaoJudicialRepository noRepository;
    private final FederacaoLedgerEntryRepository ledgerRepository;
    private final FederacaoEventoOutboxRepository outboxRepository;
    private final OutboxPublisher outboxPublisher;
    private final AuditLedgerService auditLedgerService;
    private final ObjectMapper objectMapper;
    private final AtomicReference<CachedNodes> nodesCache = new AtomicReference<>();
    private final AtomicReference<CachedHealth> healthCache = new AtomicReference<>();

    public FederalismoJudicialEngine(NoFederacaoJudicialRepository noRepository,
                                     FederacaoLedgerEntryRepository ledgerRepository,
                                     FederacaoEventoOutboxRepository outboxRepository,
                                     OutboxPublisher outboxPublisher,
                                     AuditLedgerService auditLedgerService,
                                     ObjectMapper objectMapper) {
        this.noRepository = Objects.requireNonNull(noRepository);
        this.ledgerRepository = Objects.requireNonNull(ledgerRepository);
        this.outboxRepository = Objects.requireNonNull(outboxRepository);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public FederalismoNodeResponse upsertNo(FederalismoNodeUpsertRequest request) {
        Objects.requireNonNull(request);
        NoFederacaoJudicial no = noRepository.findByCodigoTribunal(normalizeUpper(request.codigoTribunal()))
                .orElseGet(() -> new NoFederacaoJudicial(
                        request.codigoTribunal(),
                        request.nome(),
                        request.uf(),
                        request.tipoJustica(),
                        request.endpointPrincipal()
                ));
        no.setNome(request.nome());
        no.setUf(request.uf());
        no.setTipoJustica(request.tipoJustica());
        no.setEndpointPrincipal(request.endpointPrincipal());
        no.setEndpointBackup(request.endpointBackup());
        no.setKafkaBrokers(request.kafkaBrokers());
        no.setChavePublicaBase64(normalizeNullable(request.chavePublicaBase64()));
        no.setChavePublicaFingerprint(fingerprintPublicKey(request.chavePublicaBase64()));
        no.setStatusAtual(request.statusAtual() != null ? request.statusAtual() : (no.getStatusAtual() != null ? no.getStatusAtual() : StatusNoFederacao.OFFLINE_ISOLADO));
        no.setVersaoSchemaAtual(request.versaoSchemaAtual() == null ? Math.max(SCHEMA_VERSION_ATUAL, no.getVersaoSchemaAtual()) : Math.max(1L, request.versaoSchemaAtual()));
        no.setCapacidadeBacklog(request.capacidadeBacklog() == null ? Math.max(1L, no.getCapacidadeBacklog()) : Math.max(1L, request.capacidadeBacklog()));
        no.setOperacaoAutonomaAtiva(request.operacaoAutonomaAtiva() == null || request.operacaoAutonomaAtiva());
        no.setAceitaRecepcaoEventos(request.aceitaRecepcaoEventos() == null || request.aceitaRecepcaoEventos());
        no.setRegiao(request.regiao());
        no.setZona(request.zona());
        no.setPrioridadeFailover(request.prioridadeFailover() == null ? no.getPrioridadeFailover() : request.prioridadeFailover());
        no.setTopicosPermitidos(request.topicosPermitidos() == null ? Set.of(TOPIC_FEDERACAO_EVENTOS) : request.topicosPermitidos());
        no.setCapacidades(request.capacidades() == null ? Set.of("SYNC", "LEDGER", "OUTBOX", "AUDITORIA") : request.capacidades());
        NoFederacaoJudicial salvo = noRepository.save(no);
        invalidateNodeCaches();
        publicarNoAtualizado(salvo, "UPSERT");
        return FederalismoNodeResponse.of(salvo);
    }

    @Transactional
    public FederalismoNodeResponse registrarHeartbeat(FederalismoHeartbeatRequest request) {
        Objects.requireNonNull(request);
        NoFederacaoJudicial no = noRepository.findByCodigoTribunal(normalizeUpper(request.codigoTribunal()))
                .orElseThrow(() -> new IllegalArgumentException("No federativo nao encontrado"));
        no.registrarHeartbeat(request.status(), Math.max(1L, request.versaoSchemaAtual()));
        NoFederacaoJudicial salvo = noRepository.save(no);
        invalidateNodeCaches();
        publicarNoAtualizado(salvo, "HEARTBEAT");
        return FederalismoNodeResponse.of(salvo);
    }

    @PjbTransactionalBudget(operation = "federalismo.engine.listar-nos", maxMillis = 3000)
    @Transactional(readOnly = true)
    public List<FederalismoNodeResponse> listarNos() {
        CachedNodes cache = nodesCache.get();
        if (isFresh(cache)) {
            return cache.nodes();
        }
        List<FederalismoNodeResponse> nodes = noRepository.findAll().stream().map(FederalismoNodeResponse::of).toList();
        nodesCache.set(new CachedNodes(nodes, Instant.now().plus(NODE_CACHE_TTL)));
        return nodes;
    }

    @Transactional(readOnly = true)
    public Optional<FederalismoNodeResponse> buscarNo(String codigoTribunal) {
        return noRepository.findByCodigoTribunal(normalizeUpper(codigoTribunal)).map(FederalismoNodeResponse::of);
    }

    @PjbTransactionalBudget(operation = "federalismo.engine.health", maxMillis = 3000)
    @Transactional(readOnly = true)
    public FederacaoHealth healthFederacao() {
        CachedHealth cache = healthCache.get();
        if (isFresh(cache)) {
            return cache.health();
        }
        List<NoFederacaoJudicial> nos = noRepository.findAll();
        long total = nos.size();
        long online = nos.stream().filter(NoFederacaoJudicial::estaOnline).count();
        long sincronizando = nos.stream().filter(NoFederacaoJudicial::precisaSincronizar).count();
        long degradados = nos.stream().filter(no -> no.getStatusAtual() == StatusNoFederacao.DEGRADADO).count();
        long offline = total - online;
        long backlogTotal = nos.stream().mapToLong(NoFederacaoJudicial::getBacklogPendente).sum();
        double percentualOnline = total == 0L ? 0.0d : ((double) online / (double) total) * 100.0d;
        String status = percentualOnline >= 90.0d && degradados == 0L ? "SAUDAVEL"
                : percentualOnline >= 70.0d ? "DEGRADADO"
                : "CRITICO";
        FederacaoHealth health = new FederacaoHealth(total, online, offline, sincronizando, degradados, backlogTotal, percentualOnline, status, Instant.now());
        healthCache.set(new CachedHealth(health, Instant.now().plus(NODE_CACHE_TTL)));
        return health;
    }


    private void invalidateNodeCaches() {
        nodesCache.set(null);
        healthCache.set(null);
    }

    private boolean isFresh(CachedNodes cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private boolean isFresh(CachedHealth cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    @Transactional
    public FederalismoLedgerResponse registrarEventoFederado(FederalismoEventoRequest request) {
        Objects.requireNonNull(request);
        NoFederacaoJudicial no = noRepository.findByCodigoTribunal(normalizeUpper(request.tribunalCodigo()))
                .orElseGet(() -> criarNoOperacionalMinimo(request));
        if (!no.isAceitaRecepcaoEventos()) {
            throw new IllegalStateException("No federativo nao aceita recepcao de eventos");
        }
        if (!no.suportaTopico(request.topicKafka())) {
            throw new IllegalArgumentException("Topico nao permitido para o tribunal");
        }
        long schemaVersion = request.schemaVersion() == null ? SCHEMA_VERSION_ATUAL : Math.max(1L, request.schemaVersion());
        ClassificacaoConflitoFederacao conflito = classificarConflito(no, schemaVersion, request.payload());
        String payload = request.payload() == null ? "{}" : request.payload();
        String payloadHash = Hashes.sha256Hex(payload);
        String correlationId = normalizeNullable(request.correlationId());
        String idempotencyKey = normalizeNullable(request.idempotencyKey()) != null
                ? normalizeNullable(request.idempotencyKey())
                : gerarIdempotencyKey(no.getCodigoTribunal(), request.tipoEvento(), request.nupn(), payloadHash, correlationId);

        Optional<FederacaoLedgerEntry> existente = ledgerRepository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            return FederalismoLedgerResponse.of(existente.get());
        }

        long proximaSequenciaGlobal = ledgerRepository.findTopByOrderBySequenciaGlobalDesc().map(FederacaoLedgerEntry::getSequenciaGlobal).orElse(-1L) + 1L;
        long proximaSequenciaTribunal = ledgerRepository.findTopByTribunalCodigoOrderBySequenciaTribunalDesc(no.getCodigoTribunal())
                .map(FederacaoLedgerEntry::getSequenciaTribunal)
                .orElse(-1L) + 1L;
        String hashAnterior = ledgerRepository.findTopByOrderBySequenciaGlobalDesc().map(FederacaoLedgerEntry::getHashEntrada).orElse("0".repeat(64));
        String metadataJson = toJson(buildMetadata(no, request, payloadHash, conflito));
        String canonical = String.join("|",
                String.valueOf(proximaSequenciaGlobal),
                String.valueOf(proximaSequenciaTribunal),
                hashAnterior,
                no.getCodigoTribunal(),
                normalizeUpper(request.tipoEvento()),
                normalizeUpper(request.topicKafka()),
                normalizeNullable(request.nupn()) == null ? "-" : request.nupn().trim(),
                payloadHash,
                normalizeNullable(request.operadorId()) == null ? "-" : request.operadorId().trim(),
                String.valueOf(schemaVersion),
                correlationId == null ? "-" : correlationId,
                idempotencyKey
        );
        String hashEntrada = Hashes.sha256Hex(canonical);
        StatusAssinaturaFederacao statusAssinatura = Boolean.FALSE.equals(request.validarAssinatura())
                ? StatusAssinaturaFederacao.DISPENSADA
                : (no.getChavePublicaFingerprint() != null ? StatusAssinaturaFederacao.VALIDADA : StatusAssinaturaFederacao.NAO_SUPORTADA);

        FederacaoLedgerEntry entry = new FederacaoLedgerEntry(
                proximaSequenciaGlobal,
                proximaSequenciaTribunal,
                hashEntrada,
                hashAnterior,
                no.getCodigoTribunal(),
                normalizeUpper(request.tipoEvento()),
                normalizeUpper(request.topicKafka()),
                normalizeNullable(request.nupn()),
                payloadHash,
                normalizeNullable(request.operadorId()),
                schemaVersion,
                correlationId,
                idempotencyKey,
                statusAssinatura,
                conflito,
                payload.getBytes(StandardCharsets.UTF_8).length,
                metadataJson,
                Instant.now()
        );
        FederacaoLedgerEntry salvo = ledgerRepository.save(entry);
        no.incrementarBacklog();
        noRepository.save(no);

        FederacaoEventoOutbox outbox = enfileirarInterno(no, request, payload, payloadHash, correlationId, idempotencyKey, schemaVersion);
        auditLedgerService.appendSafely(
                "FEDERACAO_LEDGER_APPEND",
                "FederacaoLedgerEntry",
                String.valueOf(salvo.getId()),
                hashEntrada,
                "Registro federativo multi-tribunal"
        );
        publicarOutboxGenerico(no, salvo, outbox);
        return FederalismoLedgerResponse.of(salvo);
    }

    @Transactional
    public Optional<FederalismoOutboxResponse> processarProximoEventoPendente(String codigoTribunal) {
        Instant agora = Instant.now();
        List<FederacaoEventoOutbox> pendentes = outboxRepository
                .findTop200ByStatusAndProximaTentativaEmLessThanEqualOrderByPrioridadeDescCriadoEmAsc(StatusEventoOutboxFederacao.PENDENTE, agora);
        for (FederacaoEventoOutbox evento : pendentes) {
            if (codigoTribunal != null && !codigoTribunal.isBlank() && !normalizeUpper(codigoTribunal).equals(evento.getTribunalCodigo())) {
                continue;
            }
            NoFederacaoJudicial no = noRepository.findByCodigoTribunal(evento.getTribunalCodigo()).orElse(null);
            if (no == null) {
                evento.marcarFalha("No federativo ausente");
                outboxRepository.save(evento);
                return Optional.of(FederalismoOutboxResponse.of(evento));
            }
            if (!no.estaOnline()) {
                evento.marcarFalha("No federativo indisponivel para publicacao");
                outboxRepository.save(evento);
                no.registrarFalha("Publicacao pendente enquanto o no permanece indisponivel");
                noRepository.save(no);
                return Optional.of(FederalismoOutboxResponse.of(evento));
            }
            evento.marcarProcessando();
            evento.marcarPublicado();
            outboxRepository.save(evento);
            no.decrementarBacklog();
            no.registrarSincronizacaoConcluida(no.getBacklogPendente());
            noRepository.save(no);
            return Optional.of(FederalismoOutboxResponse.of(evento));
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<FederalismoOutboxResponse> listarPendencias(String codigoTribunal) {
        String normalized = normalizeUpper(codigoTribunal);
        List<FederacaoEventoOutbox> pendentes = outboxRepository
                .findTop200ByStatusAndProximaTentativaEmLessThanEqualOrderByPrioridadeDescCriadoEmAsc(StatusEventoOutboxFederacao.PENDENTE, Instant.now().plusSeconds(365 * 24 * 3600L));
        return pendentes.stream()
                .filter(item -> normalized == null || normalized.equals(item.getTribunalCodigo()))
                .map(FederalismoOutboxResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public long verificarIntegridadeLedger() {
        List<FederacaoLedgerEntry> entries = ledgerRepository.findTop200ByOrderBySequenciaGlobalAsc();
        FederacaoLedgerEntry anterior = null;
        for (FederacaoLedgerEntry atual : entries) {
            if (!atual.consistenteComAnterior(anterior)) {
                return atual.getSequenciaGlobal();
            }
            anterior = atual;
        }
        return -1L;
    }

    @Transactional(readOnly = true)
    public List<FederalismoLedgerResponse> consultarLedgerPorNupn(String nupn) {
        if (nupn == null || nupn.isBlank()) {
            return List.of();
        }
        return ledgerRepository.findAllByNupnOrderBySequenciaGlobalAsc(nupn.trim()).stream()
                .map(FederalismoLedgerResponse::of)
                .toList();
    }

    @Transactional
    public void registrarProcessoAjuizado(Processo processo) {
        if (processo == null || processo.getNumeroUnificado() == null || processo.getNumeroUnificado().isBlank()) {
            return;
        }
        String tribunalCodigo = resolveTribunalCodigo(processo);
        if (noRepository.findByCodigoTribunal(normalizeUpper(tribunalCodigo)).isEmpty()) {
            upsertNo(new FederalismoNodeUpsertRequest(
                    tribunalCodigo,
                    "Tribunal " + tribunalCodigo,
                    resolveUf(processo),
                    processo.getTipoJustica() != null ? processo.getTipoJustica() : com.tcc.pjb.backend.domain.enums.TipoJustica.ESTADUAL,
                    "https://" + tribunalCodigo.toLowerCase() + ".pjb.local",
                    null,
                    null,
                    null,
                    StatusNoFederacao.ONLINE,
                    SCHEMA_VERSION_ATUAL,
                    20000L,
                    true,
                    true,
                    "BRASIL",
                    "DEFAULT",
                    50,
                    Set.of(TOPIC_FEDERACAO_EVENTOS),
                    Set.of("SYNC", "LEDGER", "OUTBOX", "PROCESSO")
            ));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("processoId", processo.getId());
        payload.put("numeroUnificado", processo.getNumeroUnificado());
        payload.put("tipoJustica", processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null);
        payload.put("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
        payload.put("classeProcessual", processo.getClasseProcessual());
        payload.put("assunto", processo.getAssunto());
        payload.put("parteAutora", processo.getParteAutoraNome());
        payload.put("parteReu", processo.getParteReuNome());
        payload.put("valorCausa", processo.getValorCausa());
        payload.put("ocorridoEm", Instant.now().toString());
        registrarEventoFederado(new FederalismoEventoRequest(
                tribunalCodigo,
                TOPIC_FEDERACAO_EVENTOS,
                "PROCESSO_AJUIZADO",
                processo.getNumeroUnificado(),
                processo.getUsuario() != null && processo.getUsuario().getId() != null ? String.valueOf(processo.getUsuario().getId()) : null,
                "proc:" + processo.getId(),
                null,
                SCHEMA_VERSION_ATUAL,
                100,
                true,
                toJson(payload),
                Map.of("aggregateType", "Processo", "aggregateId", processo.getId())
        ));
    }

    private String resolveTribunalCodigo(Processo processo) {
        String explicit = firstNonBlank(
                processo.getTribunalCodigoRoteado(),
                processo.getTribunal(),
                processo.getJurisdicao() != null ? processo.getJurisdicao().getCodigo() : null,
                processo.getJurisdicao() != null ? processo.getJurisdicao().getSigla() : null
        );
        if (explicit != null) {
            return explicit;
        }
        return defaultTribunalCodigo(processo);
    }

    private String resolveUf(Processo processo) {
        return firstNonBlank(
                processo.getUf(),
                processo.getJurisdicao() != null ? processo.getJurisdicao().getEstado() : null,
                processo.getJurisdicao() != null ? processo.getJurisdicao().getUf() : null
        );
    }

    private FederacaoEventoOutbox enfileirarInterno(NoFederacaoJudicial no,
                                                    FederalismoEventoRequest request,
                                                    String payload,
                                                    String payloadHash,
                                                    String correlationId,
                                                    String idempotencyKey,
                                                    long schemaVersion) {
        Optional<FederacaoEventoOutbox> existente = outboxRepository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            return existente.get();
        }
        FederacaoEventoOutbox event = new FederacaoEventoOutbox(
                UUID.randomUUID(),
                no.getCodigoTribunal(),
                normalizeUpper(request.topicKafka()),
                normalizeUpper(request.tipoEvento()),
                payload,
                payloadHash,
                idempotencyKey,
                correlationId,
                schemaVersion,
                request.prioridade() == null ? 50 : request.prioridade()
        );
        return outboxRepository.save(event);
    }

    private void publicarNoAtualizado(NoFederacaoJudicial no, String origem) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("codigoTribunal", no.getCodigoTribunal());
        payload.put("statusAtual", no.getStatusAtual().name());
        payload.put("versaoSchemaAtual", no.getVersaoSchemaAtual());
        payload.put("ultimaHeartbeatEm", no.getUltimaHeartbeatEm() != null ? no.getUltimaHeartbeatEm().toString() : null);
        payload.put("backlogPendente", no.getBacklogPendente());
        payload.put("origem", origem);
        outboxPublisher.enqueue(
                "federacao:no:" + no.getCodigoTribunal(),
                EVT_FEDERACAO_NO_ATUALIZADO,
                payload,
                Map.of("source", "federalismo_judicial"),
                "federacaoNo:" + no.getCodigoTribunal() + ":" + origem + ":" + (no.getUltimaHeartbeatEm() != null ? no.getUltimaHeartbeatEm().toEpochMilli() : 0L),
                "NoFederacaoJudicial",
                no.getCodigoTribunal()
        );
    }

    private void publicarOutboxGenerico(NoFederacaoJudicial no, FederacaoLedgerEntry ledger, FederacaoEventoOutbox event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tribunalCodigo", no.getCodigoTribunal());
        payload.put("sequenciaGlobal", ledger.getSequenciaGlobal());
        payload.put("sequenciaTribunal", ledger.getSequenciaTribunal());
        payload.put("hashEntrada", ledger.getHashEntrada());
        payload.put("nupn", ledger.getNupn());
        payload.put("topicKafka", ledger.getTopicKafka());
        payload.put("idempotencyKey", ledger.getIdempotencyKey());
        payload.put("outboxId", event.getId());
        outboxPublisher.enqueue(
                "federacao:evento:" + no.getCodigoTribunal(),
                EVT_FEDERACAO_LEDGER_REGISTRADO,
                payload,
                Map.of("source", "federalismo_judicial"),
                "federacaoLedger:" + ledger.getIdempotencyKey(),
                "FederacaoLedgerEntry",
                String.valueOf(ledger.getId())
        );
        outboxPublisher.enqueue(
                "federacao:outbox:" + no.getCodigoTribunal(),
                EVT_FEDERACAO_OUTBOX_PENDENTE,
                Map.of(
                        "outboxId", event.getId(),
                        "tribunalCodigo", no.getCodigoTribunal(),
                        "topicKafka", event.getTopicKafka(),
                        "prioridade", event.getPrioridade(),
                        "tentativas", event.getTentativas()
                ),
                Map.of("source", "federalismo_judicial"),
                "federacaoOutbox:" + event.getIdempotencyKey(),
                "FederacaoEventoOutbox",
                String.valueOf(event.getId())
        );
    }

    private NoFederacaoJudicial criarNoOperacionalMinimo(FederalismoEventoRequest request) {
        String codigo = normalizeUpper(request.tribunalCodigo());
        if (codigo == null) {
            throw new IllegalArgumentException("Codigo do tribunal obrigatorio");
        }
        String topic = normalizeUpper(request.topicKafka());
        NoFederacaoJudicial no = new NoFederacaoJudicial(
                codigo,
                "Tribunal " + codigo,
                null,
                TipoJustica.ESTADUAL,
                "https://" + codigo.toLowerCase() + ".pjb.local"
        );
        no.setStatusAtual(StatusNoFederacao.ONLINE);
        no.setVersaoSchemaAtual(SCHEMA_VERSION_ATUAL);
        no.setCapacidadeBacklog(20000L);
        no.setOperacaoAutonomaAtiva(true);
        no.setAceitaRecepcaoEventos(true);
        no.setRegiao("BRASIL");
        no.setZona("DEFAULT");
        no.setPrioridadeFailover(50);
        no.setTopicosPermitidos(topic == null ? Set.of(TOPIC_FEDERACAO_EVENTOS) : Set.of(topic));
        no.setCapacidades(Set.of("SYNC", "LEDGER", "OUTBOX", "PROCESSO"));
        NoFederacaoJudicial salvo = noRepository.saveAndFlush(no);
        invalidateNodeCaches();
        return salvo;
    }

    private Map<String, Object> buildMetadata(NoFederacaoJudicial no, FederalismoEventoRequest request, String payloadHash, ClassificacaoConflitoFederacao conflito) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCodigo", no.getCodigoTribunal());
        metadata.put("tipoJustica", no.getTipoJustica() != null ? no.getTipoJustica().name() : null);
        metadata.put("schemaVersionNo", no.getVersaoSchemaAtual());
        metadata.put("schemaVersionEvento", request.schemaVersion() == null ? SCHEMA_VERSION_ATUAL : request.schemaVersion());
        metadata.put("payloadHash", payloadHash);
        metadata.put("conflito", conflito.name());
        metadata.put("topicoPermitido", no.suportaTopico(request.topicKafka()));
        metadata.put("capacidadeOutbox", no.suportaCapacidade("OUTBOX"));
        metadata.put("aceitaRecepcaoEventos", no.isAceitaRecepcaoEventos());
        metadata.put("fingerprintChave", no.getChavePublicaFingerprint());
        metadata.put("clockLogico", no.getClockLogico());
        return metadata;
    }

    private ClassificacaoConflitoFederacao classificarConflito(NoFederacaoJudicial no, long schemaVersion, String payload) {
        if (schemaVersion > no.getVersaoSchemaAtual()) {
            return ClassificacaoConflitoFederacao.ESQUEMA;
        }
        if (payload == null || payload.isBlank()) {
            return ClassificacaoConflitoFederacao.PAYLOAD;
        }
        if (payload.getBytes(StandardCharsets.UTF_8).length > 512_000) {
            return ClassificacaoConflitoFederacao.CRITICO;
        }
        if (no.getBacklogPendente() >= no.getCapacidadeBacklog()) {
            return ClassificacaoConflitoFederacao.SINCRONIZACAO;
        }
        return ClassificacaoConflitoFederacao.NENHUM;
    }

    private String fingerprintPublicKey(String chavePublicaBase64) {
        String normalized = normalizeNullable(chavePublicaBase64);
        if (normalized == null) {
            return null;
        }
        try {
            return Hashes.sha256HexBytes(Base64.getDecoder().decode(normalized));
        } catch (Exception ex) {
            return Hashes.sha256Hex(normalized);
        }
    }

    private static String gerarIdempotencyKey(String tribunalCodigo, String tipoEvento, String nupn, String payloadHash, String correlationId) {
        return "fed:" + Hashes.sha256Hex(String.join("|",
                tribunalCodigo == null ? "-" : tribunalCodigo.trim(),
                tipoEvento == null ? "-" : tipoEvento.trim(),
                nupn == null ? "-" : nupn.trim(),
                payloadHash == null ? "-" : payloadHash,
                correlationId == null ? "-" : correlationId
        ));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("json_error", ex);
        }
    }

    private String defaultTribunalCodigo(Processo processo) {
        if (processo.getTipoJustica() == null) {
            return "PJB";
        }
        return switch (processo.getTipoJustica()) {
            case ESTADUAL -> "TJ" + Objects.requireNonNullElse(resolveUf(processo), "BR");
            case FEDERAL -> "TRF";
            case TRABALHO -> "TRT";
            case ELEITORAL -> "TRE";
            case MILITAR_ESTADUAL -> "TJM";
            case MILITAR_FEDERAL -> "STM";
            case SUPERIOR -> "STJ";
        };
    }

    private static String normalizeUpper(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toUpperCase();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeNullable(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record FederacaoHealth(
            long totalNos,
            long nosOnline,
            long nosOffline,
            long nosSincronizando,
            long nosDegradados,
            long backlogTotal,
            double percentualOnline,
            String status,
            Instant verificadoEm
    ) {
    }

    private record CachedNodes(List<FederalismoNodeResponse> nodes, Instant expiresAt) {
    }

    private record CachedHealth(FederacaoHealth health, Instant expiresAt) {
    }
}
