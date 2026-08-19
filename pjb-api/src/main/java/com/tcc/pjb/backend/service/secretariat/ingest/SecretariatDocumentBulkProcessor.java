package com.tcc.pjb.backend.service.secretariat.ingest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioResolver;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskResolver;
import com.tcc.pjb.backend.core.kernel.process.events.ProcessEventAppendedEvent;
import com.tcc.pjb.backend.core.storage.ObjectReadResult;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.secretariat.DocumentoTriageSignal;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.platform.unified.eventsourcing.JudiciarioEventSourcingEngine;
import com.tcc.pjb.backend.repository.secretariat.DocumentoTriageSignalRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeResolver;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationResolver;
import com.tcc.pjb.backend.service.secretariat.triage.DocumentUrgencyClassifier;
import com.tcc.pjb.backend.service.secretariat.triage.PdfTextSnippetExtractor;
import com.tcc.pjb.backend.service.secretariat.triage.SecretariatWorkloadBalancer;
import com.tcc.pjb.backend.service.secretariat.triage.SecretariatWorkloadProfile;
import com.tcc.pjb.backend.service.secretariat.triage.TriageRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.triage.TriageRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.triage.TriageSignal;
import com.tcc.pjb.backend.service.secretariat.triage.UrgencyLevel;

@Component
public class SecretariatDocumentBulkProcessor {

  private final ObjectMapper mapper;
  private final ProcessoRepository processoRepository;
  private final WorkItemRepository workItemRepository;
  private final DocumentoTriageSignalRepository triageRepository;
  private final ObjectStoragePort storage;
  private final PdfTextSnippetExtractor textExtractor;
  private final DocumentUrgencyClassifier classifier;
  private final TriageRoutingResolver triageRoutingResolver;
  private final OutboxPublisher outbox;
  private final SecretariatQueueProjectionService projectionService;
  private final SecretariatWorkloadBalancer workloadBalancer;
  private final ForumDeskResolver deskResolver;
  private final ForumDeskPortfolioResolver deskPortfolioResolver;
  private final SecretariatFlowBridgeResolver flowBridgeResolver;
  private final SecretariatJudicialIntegrationResolver integrationResolver;
  private final Executor executor;
  private final Semaphore globalPermits;
  private final int perInboxPermits;
  private final ConcurrentHashMap<String, Semaphore> inboxPermits = new ConcurrentHashMap<>();

  public SecretariatDocumentBulkProcessor(
      ObjectMapper mapper,
      ProcessoRepository processoRepository,
      WorkItemRepository workItemRepository,
      DocumentoTriageSignalRepository triageRepository,
      ObjectStoragePort storage,
      PdfTextSnippetExtractor textExtractor,
      DocumentUrgencyClassifier classifier,
      TriageRoutingResolver triageRoutingResolver,
      OutboxPublisher outbox,
      SecretariatQueueProjectionService projectionService,
      SecretariatWorkloadBalancer workloadBalancer,
      ForumDeskResolver deskResolver,
      ForumDeskPortfolioResolver deskPortfolioResolver,
      SecretariatFlowBridgeResolver flowBridgeResolver,
      SecretariatJudicialIntegrationResolver integrationResolver,
      Environment env,
      @org.springframework.beans.factory.annotation.Qualifier("pjbBurstExecutor") java.util.concurrent.Executor executor
  ) {
    this.mapper = Objects.requireNonNull(mapper);
    this.processoRepository = Objects.requireNonNull(processoRepository);
    this.workItemRepository = Objects.requireNonNull(workItemRepository);
    this.triageRepository = Objects.requireNonNull(triageRepository);
    this.storage = Objects.requireNonNull(storage);
    this.textExtractor = Objects.requireNonNull(textExtractor);
    this.classifier = Objects.requireNonNull(classifier);
    this.triageRoutingResolver = Objects.requireNonNull(triageRoutingResolver);
    this.outbox = Objects.requireNonNull(outbox);
    this.projectionService = Objects.requireNonNull(projectionService);
    this.workloadBalancer = Objects.requireNonNull(workloadBalancer);
    this.deskResolver = Objects.requireNonNull(deskResolver);
    this.deskPortfolioResolver = Objects.requireNonNull(deskPortfolioResolver);
    this.flowBridgeResolver = Objects.requireNonNull(flowBridgeResolver);
    this.integrationResolver = Objects.requireNonNull(integrationResolver);
    this.executor = Objects.requireNonNull(executor);

    int gp = Integer.parseInt(env.getProperty("pjb.secretariat.ingest.maxParallel", "32"));
    if (gp < 1 || gp > 5000) {
      throw new IllegalArgumentException("ingest maxParallel out of range");
    }
    int pp = Integer.parseInt(env.getProperty("pjb.secretariat.ingest.maxParallelPerInbox", "4"));
    if (pp < 1 || pp > 5000) {
      throw new IllegalArgumentException("ingest maxParallelPerInbox out of range");
    }
    this.globalPermits = new Semaphore(gp, true);
    this.perInboxPermits = pp;
  }

  public void enqueue(ProcessEventAppendedEvent evt) {
    if (evt == null) {
      return;
    }
    executor.execute(() -> guardedProcess(evt));
  }

  private void guardedProcess(ProcessEventAppendedEvent evt) {
    boolean g = false;
    Semaphore inbox = null;
    boolean i = false;
    try {
      globalPermits.acquire();
      g = true;

      long shard = evt.processoId() == null ? 0L : (evt.processoId() % 1024L);
      String key = "S" + shard;
      inbox = inboxPermits.computeIfAbsent(key, __ -> new Semaphore(perInboxPermits, true));
      inbox.acquire();
      i = true;

      process(evt);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      if (i) {
        inbox.release();
      }
      if (g) {
        globalPermits.release();
      }
    }
  }

  private void process(ProcessEventAppendedEvent evt) {
    JudiciarioEventSourcingEngine.DocumentosJuntados payload = readPayload(evt.payloadJson());
    if (payload == null || payload.documentos() == null || payload.documentos().isEmpty()) {
      return;
    }

    Long pid = evt.processoId() != null ? evt.processoId() : payload.processoId();

    Processo processo = processoRepository.findById(pid).orElse(null);
    if (processo == null) {
      return;
    }

    Jurisdicao j = processo.getJurisdicao();
    String inboxKey = resolveInboxKey(processo);
    String templateCode = "DOC_BULK:" + evt.payloadHash();

    WorkItem workItem = workItemRepository
        .findFirstByProcesso_IdAndTemplateCodeAndStatusNot(pid, templateCode, WorkItemStatus.CANCELADO)
        .orElse(null);

    if (workItem == null) {
      workItem = createWorkItem(processo, j, inboxKey, templateCode, payload);
      workItem = workItemRepository.save(workItem);
    } else {
      if (workItem.getInboxKey() == null || !workItem.getInboxKey().equals(inboxKey)) {
        workItem.setInboxKey(inboxKey);
      }
      if (workItem.getQueueCode() == null) {
        workItem.setQueueCode("SECRETARIA");
      }
    }

    AggregateTriage agg = triageBulk(pid, workItem.getId(), payload.documentos());
    TriageRoutingProfile triageProfile = triageRoutingResolver.resolve(processo, inboxKey, agg.signal());
    SecretariatWorkloadProfile workloadProfile = workloadBalancer.resolve(triageProfile.inboxKey(), triageProfile);
    var deskKey = deskResolver.resolveForProcess(processo);
    var deskPortfolio = deskPortfolioResolver.resolve(deskKey);
    SecretariatFlowBridgeProfile bridgeProfile = flowBridgeResolver.resolve(
        triageProfile.inboxKey(),
        workloadProfile.effectiveQueueCode(),
        workTitle(triageProfile, workloadProfile),
        agg.tags(),
        deskPortfolio
    );
    SecretariatJudicialIntegrationProfile integrationProfile = integrationResolver.resolve(
        triageProfile.inboxKey(),
        workloadProfile.effectiveQueueCode(),
        workTitle(triageProfile, workloadProfile),
        agg.tags(),
        deskPortfolio,
        bridgeProfile
    );

    workItem.setInboxKey(triageProfile.inboxKey());
    workItem.setQueueCode(workloadProfile.effectiveQueueCode());
    workItem.setPrioridade(workloadProfile.effectivePriority());
    workItem.setBlocking(triageProfile.blocking());
    workItem.setDueAt(Instant.now().plus(workloadProfile.effectiveDueIn()));
    workItem.setTitulo(limit220(workTitle(triageProfile, workloadProfile)));
    workItem.setBaseLegal(buildBaseLegal(agg.signal(), triageProfile));
    if (processo.getFaseAtual() != null) {
      workItem.setFaseOrigem(processo.getFaseAtual());
    }

    String desc = "Juntada em lote: " + payload.documentos().size() + " documento(s)";
    if (!agg.tags().isEmpty()) {
      desc += " | tags: " + String.join(", ", agg.tags());
    }
    desc += " | score=" + agg.score();
    desc += " | fila=" + workloadProfile.effectiveQueueCode();
    desc += " | eixo=" + triageProfile.deskAxis();
    desc += " | carga=" + workloadProfile.workloadBand();
    desc += " | perfil=" + triageProfile.descriptor();
    desc += " | workload=" + workloadProfile.descriptor();
    desc += " | bridge=" + bridgeProfile.descriptor();
    desc += " | integration=" + integrationProfile.descriptor();
    desc += " | connector=" + integrationProfile.connectorSystem();
    desc += " | workflow=" + integrationProfile.connectorWorkflowMode();
    desc += " | fallback=" + integrationProfile.fallbackMode();
    desc += " | contingencyDesk=" + integrationProfile.contingencyDesk();
    workItem.setDescricao(desc);

    workItemRepository.save(workItem);

    projectionService.upsert(workItem, agg.score(), agg.tags(), triageProfile, workloadProfile);

    var payloadOut = new java.util.LinkedHashMap<String, Object>();
    payloadOut.put("workItemId", String.valueOf(workItem.getId()));
    payloadOut.put("processoId", pid);
    payloadOut.put("type", workItem.getType().name());
    payloadOut.put("status", workItem.getStatus().name());
    payloadOut.put("prioridade", workItem.getPrioridade());
    if (workItem.getDueAt() != null) {
      payloadOut.put("dueAt", workItem.getDueAt().toString());
    }
    payloadOut.put("titulo", workItem.getTitulo());
    payloadOut.put("descricao", workItem.getDescricao());
    payloadOut.put("tags", agg.tags());
    payloadOut.put("score", agg.score());
    payloadOut.put("triageProfile", triageProfile.toMap());
    payloadOut.put("triageFingerprint", triageProfile.fingerprint());
    payloadOut.put("workloadProfile", workloadProfile.toMap());
    payloadOut.put("workloadFingerprint", workloadProfile.fingerprint());
    payloadOut.put("flowBridge", bridgeProfile.toMap());
    payloadOut.put("flowBridgeDescriptor", bridgeProfile.descriptor());
    payloadOut.put("integrationProfile", integrationProfile.toMap());
    payloadOut.put("integrationDescriptor", integrationProfile.descriptor());
    payloadOut.put("connectorSystem", integrationProfile.connectorSystem());
    payloadOut.put("connectorWorkflowMode", integrationProfile.connectorWorkflowMode());
    payloadOut.put("fallbackMode", integrationProfile.fallbackMode());
    payloadOut.put("contingencyDesk", integrationProfile.contingencyDesk());
    payloadOut.put("replayQueue", integrationProfile.replayQueue());
    payloadOut.put("evidenceRetentionPolicy", integrationProfile.evidenceRetentionPolicy());
    payloadOut.put("manualSubmissionDesk", integrationProfile.manualSubmissionDesk());
    payloadOut.put("telemetryMode", integrationProfile.telemetryMode());
    payloadOut.put("telemetryChannel", integrationProfile.telemetryChannel());
    payloadOut.put("deadLetterQueue", integrationProfile.deadLetterQueue());
    payloadOut.put("reconciliationDesk", integrationProfile.reconciliationDesk());
    payloadOut.put("submissionAuditMode", integrationProfile.submissionAuditMode());
    payloadOut.put("protocolSlaBucket", integrationProfile.protocolSlaBucket());
    payloadOut.put("escalationDesk", integrationProfile.escalationDesk());
    payloadOut.put("receiptAuditDesk", integrationProfile.receiptAuditDesk());
    payloadOut.put("proofBundleMode", integrationProfile.proofBundleMode());
    payloadOut.put("reconciliationWindow", integrationProfile.reconciliationWindow());
    payloadOut.put("connectorWarnings", integrationProfile.connectorWarnings());

    outbox.enqueueSecretariatLive(workItem.getInboxKey(), "WORK_ITEM_UPDATED", Instant.now(), payloadOut);
  }



  private String workTitle(TriageRoutingProfile triageProfile, SecretariatWorkloadProfile workloadProfile) {
    String base = triageProfile.workTitle() == null || triageProfile.workTitle().isBlank() ? "Triagem de secretaria" : triageProfile.workTitle().trim();
    if (workloadProfile.fastTrackDesk()) {
      return base + " | fast-track";
    }
    if (workloadProfile.saturated()) {
      return base + " | carga crítica";
    }
    if (workloadProfile.pressured()) {
      return base + " | fila sob pressão";
    }
    return base;
  }

  private WorkItem createWorkItem(Processo processo, Jurisdicao j, String inboxKey, String templateCode,
                                  JudiciarioEventSourcingEngine.DocumentosJuntados payload) {
    WorkItem.WorkItemBuilder b = WorkItem.builder()
        .processo(processo)
        .type(WorkItemType.JUNTADA)
        .status(WorkItemStatus.PENDENTE)
        .titulo("Analisar juntada em lote")
        .descricao("Juntada em lote: " + payload.documentos().size() + " documento(s)")
        .prioridade(3)
        .queueCode("SECRETARIA")
        .inboxKey(inboxKey)
        .assignedRole(TipoUsuario.SERVIDOR_FORUM)
        .templateCode(templateCode)
        .dueAt(Instant.now().plus(Duration.ofDays(1)));

    if (j != null) {
      if (j.getUf() != null) {
        b.uf(j.getUf());
      }
      if (j.getCidade() != null) {
        b.comarca(j.getCidade());
      }
    }
    return b.build();
  }

  private AggregateTriage triageBulk(Long processoId, Long workItemId,
                                     List<JudiciarioEventSourcingEngine.DocumentoMetadata> docs) {
    int best = 0;
    Set<String> tags = new LinkedHashSet<>();

    for (JudiciarioEventSourcingEngine.DocumentoMetadata d : docs) {
      if (d == null || d.docId() == null) {
        continue;
      }

      DocumentoTriageSignal existing = triageRepository.findFirstByDocId(d.docId()).orElse(null);
      if (existing != null) {
        best = Math.max(best, Optional.ofNullable(existing.getScore()).orElse(0));
        tags.addAll(parseJsonList(existing.getTagsJson()));
        continue;
      }

      TriageSignal signal = triageOne(d);
      best = Math.max(best, signal.score());
      tags.addAll(signal.tags());

      DocumentoTriageSignal entity = DocumentoTriageSignal.builder()
          .id(UUID.randomUUID())
          .docId(d.docId())
          .processoId(processoId)
          .workItemId(workItemId)
          .urgencyLevel(signal.level().name())
          .score(signal.score())
          .tagsJson(writeJson(signal.tags()))
          .keywords(String.join(",", signal.keywords()))
          .rationale(signal.rationale())
          .createdAt(Instant.now())
          .build();
      triageRepository.save(entity);

      if (best >= 100) {
        break;
      }
    }

    return new AggregateTriage(best, List.copyOf(tags), signalFrom(best, tags));
  }

  private TriageSignal triageOne(JudiciarioEventSourcingEngine.DocumentoMetadata d) {
    String key = resolveStorageKey(d.storageUri());
    if (key == null || key.isBlank()) {
      return new TriageSignal(UrgencyLevel.LOW, 0, List.of("SEM_KEY"), List.of(), "storageKey ausente");
    }
    try {
      ObjectReadResult rr = storage.get(key);
      try (InputStream in = rr.resource().getInputStream()) {
        String snippet = textExtractor.extract(in, 6, 40_000);
        return classifier.classify(snippet);
      }
    } catch (Exception e) {
      return new TriageSignal(UrgencyLevel.LOW, 0, List.of("FALHA_LEITURA"), List.of(), "erro ao ler documento");
    }
  }

  private TriageSignal signalFrom(int score, Set<String> tags) {
    UrgencyLevel level = score >= 85 ? UrgencyLevel.CRITICAL : score >= 65 ? UrgencyLevel.HIGH : score >= 35 ? UrgencyLevel.MEDIUM : UrgencyLevel.LOW;
    return new TriageSignal(level, score, List.copyOf(tags), List.of(), "bulk aggregate");
  }

  private String buildBaseLegal(TriageSignal signal, TriageRoutingProfile profile) {
    LinkedHashSet<String> norms = new LinkedHashSet<>();
    if (signal.hasAnyTag("TUTELA_URGENCIA", "CPC_300", "UTI", "MEDICAMENTO", "SAUDE")) {
      norms.add("CPC art. 300");
    }
    if (signal.hasAnyTag("HABEAS_CORPUS", "PRISAO", "CPP_310", "ALVARA")) {
      norms.add("CPP art. 310");
    }
    if (signal.hasAnyTag("MEDIDA_PROTETIVA", "VIOLENCIA_DOMESTICA")) {
      norms.add("Lei 11.340/2006");
    }
    if (profile.secrecyReviewRequired()) {
      norms.add("Revisão de sigilo");
    }
    return norms.isEmpty() ? null : String.join(" | ", norms);
  }

  private static String limit220(String raw) {
    if (raw == null || raw.isBlank()) {
      return "Analisar juntada em lote";
    }
    String value = raw.trim();
    return value.length() <= 220 ? value : value.substring(0, 220);
  }

  private JudiciarioEventSourcingEngine.DocumentosJuntados readPayload(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return mapper.readValue(json, JudiciarioEventSourcingEngine.DocumentosJuntados.class);
    } catch (Exception e) {
      return null;
    }
  }

  private String resolveInboxKey(Processo processo) {
    if (processo == null) {
      return "SEC:UNKNOWN:1G:COM:XX:-:-";
    }
    return deskResolver.resolveForProcess(processo).inboxKey();
  }

  private String writeJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception e) {
      return "[]";
    }
  }

  private List<String> parseJsonList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return mapper.readValue(json, new TypeReference<List<String>>() {
      });
    } catch (Exception e) {
      return List.of();
    }
  }

  private static String resolveStorageKey(String uriOrKey) {
    if (uriOrKey == null || uriOrKey.isBlank()) {
      return null;
    }
    String v = uriOrKey.trim();
    if (v.startsWith("http://") || v.startsWith("https://")) {
      try {
        URI u = URI.create(v);
        String p = u.getPath();
        int idx = p != null ? p.indexOf("/objects/") : -1;
        if (idx >= 0) {
          return p.substring(idx + "/objects/".length());
        }
      } catch (Exception ignored) {
      }
      return null;
    }
    if (v.startsWith("/objects/")) {
      return v.substring("/objects/".length());
    }
    return v;
  }

  private record AggregateTriage(int score, List<String> tags, TriageSignal signal) {
  }
}
