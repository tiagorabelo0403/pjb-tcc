package com.tcc.pjb.backend.service.secretariat.query.queue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatCoverageSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatExceptionDeskSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatFormalCatalogSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatGovernanceSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatInboxSummaryDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueItemDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelSnapshotDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import com.tcc.pjb.backend.service.secretariat.governance.SecretariatGovernanceService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalActionModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalDeskModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalTransactionModelService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatDeskLoadProfile;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatInstitutionalAlignmentService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatJudicialReferenceModelService;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeResolver;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatHearingMediaLaneService;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationResolver;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatMigrationLaneService;
import com.tcc.pjb.backend.service.ui.UiHintService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SecretariatQueueQueryService {

  private final SecretariatQueueItemRepository repo;
  private final ProcessoRepository processoRepository;
  private final ObjectMapper mapper;
  private final UiHintService uiHints;
  private final SecretariatQueueInboxContextResolver inboxContextResolver;
  private final SecretariatFlowBridgeResolver flowBridgeResolver;
  private final SecretariatJudicialIntegrationResolver integrationResolver;
  private final SecretariatGovernanceService governanceService;
  private final SecretariatQueueSummaryAssembler summaryAssembler;
  private final SecretariatJudicialReferenceModelService referenceModelService;
  private final SecretariatInstitutionalAlignmentService institutionalAlignmentService;
  private final SecretariatOperationalDeskModelService operationalDeskModelService;
  private final SecretariatOperationalActionModelService operationalActionModelService;
  private final SecretariatOperationalTransactionModelService operationalTransactionModelService;
  private final SecretariatMigrationLaneService migrationLaneService;
  private final SecretariatHearingMediaLaneService hearingMediaLaneService;
  private final SecretariatQueuePanelRowProjectionSupport panelRowProjectionSupport;
  private final SecretariatQueuePanelProjectionSupport panelProjectionSupport;
  private final SecretariatQueueAgendaProjectionSupport agendaProjectionSupport;

  public SecretariatQueueQueryService(
      SecretariatQueueItemRepository repo,
      ProcessoRepository processoRepository,
      ObjectMapper mapper,
      UiHintService uiHints,
      SecretariatQueueInboxContextResolver inboxContextResolver,
      SecretariatFlowBridgeResolver flowBridgeResolver,
      SecretariatJudicialIntegrationResolver integrationResolver,
      SecretariatGovernanceService governanceService,
      SecretariatQueueSummaryAssembler summaryAssembler,
      SecretariatJudicialReferenceModelService referenceModelService,
      SecretariatInstitutionalAlignmentService institutionalAlignmentService,
      SecretariatOperationalDeskModelService operationalDeskModelService,
      SecretariatOperationalActionModelService operationalActionModelService,
      SecretariatOperationalTransactionModelService operationalTransactionModelService,
      SecretariatMigrationLaneService migrationLaneService,
      SecretariatHearingMediaLaneService hearingMediaLaneService
  ) {
    this.repo = Objects.requireNonNull(repo);
    this.processoRepository = Objects.requireNonNull(processoRepository);
    this.mapper = Objects.requireNonNull(mapper);
    this.uiHints = Objects.requireNonNull(uiHints);
    this.inboxContextResolver = Objects.requireNonNull(inboxContextResolver);
    this.flowBridgeResolver = Objects.requireNonNull(flowBridgeResolver);
    this.integrationResolver = Objects.requireNonNull(integrationResolver);
    this.governanceService = Objects.requireNonNull(governanceService);
    this.summaryAssembler = Objects.requireNonNull(summaryAssembler);
    this.referenceModelService = Objects.requireNonNull(referenceModelService);
    this.institutionalAlignmentService = Objects.requireNonNull(institutionalAlignmentService);
    this.operationalDeskModelService = Objects.requireNonNull(operationalDeskModelService);
    this.operationalActionModelService = Objects.requireNonNull(operationalActionModelService);
    this.operationalTransactionModelService = Objects.requireNonNull(operationalTransactionModelService);
    this.migrationLaneService = Objects.requireNonNull(migrationLaneService);
    this.hearingMediaLaneService = Objects.requireNonNull(hearingMediaLaneService);
    this.panelRowProjectionSupport = new SecretariatQueuePanelRowProjectionSupport();
    this.panelProjectionSupport = new SecretariatQueuePanelProjectionSupport();
    this.agendaProjectionSupport = new SecretariatQueueAgendaProjectionSupport(panelProjectionSupport);
  }

  public Page<SecretariatQueueItemDto> list(String inboxKey, Collection<String> statuses, Pageable pageable) {
    SecretariatQueueInboxContext context = inboxContextResolver.resolve(inboxKey, statuses);
    Page<SecretariatQueueItem> page = repo.listInbox(context.inboxKey(), context.statuses(), pageable);
    List<Long> processoIds = page.getContent().stream().map(SecretariatQueueItem::getProcessoId).filter(Objects::nonNull).distinct().toList();
    Map<Long, Processo> processosPorId = processoIds.isEmpty() ? Map.of()
        : processoRepository.findAllById(processoIds).stream().collect(java.util.stream.Collectors.toMap(Processo::getId, p -> p));
    return page.map(item -> toDto(item, context.loadProfile(), context.deskProfile(), context.portfolio(), context.inboxDescriptor(), context.dashboardBucket(), context.inboxProfile(), processosPorId.get(item.getProcessoId())));
  }

  public SecretariatInboxSummaryDto summary(String inboxKey, Collection<String> statuses) {
    SecretariatQueueInboxContext context = inboxContextResolver.resolve(inboxKey, statuses);
    SecretariatFlowBridgeProfile bridgeProfile = flowBridgeResolver.resolve(context.inboxKey(), null, null, List.of(), context.portfolio());
    SecretariatJudicialIntegrationProfile integrationProfile = integrationResolver.resolve(context.inboxKey(), null, null, List.of(), context.portfolio(), bridgeProfile);
    SecretariatQueueSummaryProjection summaryProjection = summaryAssembler.assemble(context, bridgeProfile, integrationProfile);
    return new SecretariatInboxSummaryDto(
        context.inboxKey(),
        context.inboxDescriptor(),
        context.portfolio().operationalDescriptor(),
        context.loadProfile().loadBand(),
        context.loadProfile().responseMode(),
        context.loadProfile().totalItems(),
        context.loadProfile().overdueItems(),
        context.loadProfile().criticalItems(),
        context.loadProfile().dueWithin24hItems(),
        context.loadProfile().rebalanceSuggested() || context.deskProfile().forceRedistribution(),
        context.deskProfile().dominantDeskAxis(),
        context.deskProfile().redistributionDesk(),
        context.deskProfile().gabineteSupportDesk(),
        context.deskProfile().coordinationMode(),
        context.deskProfile().secrecyPressure(),
        context.deskProfile().hearingPressure(),
        bridgeProfile.downstreamAxis(),
        bridgeProfile.bridgeMode(),
        bridgeProfile.recursalDesk(),
        bridgeProfile.admissibilityDesk(),
        integrationProfile.targetSystem(),
        integrationProfile.protocolDesk(),
        integrationProfile.syncMode(),
        integrationProfile.reviewDesk(),
        integrationProfile.connectorId(),
        integrationProfile.ackChannel(),
        integrationProfile.replayDesk(),
        integrationProfile.retryMode(),
        integrationProfile.evidencePolicy(),
        integrationProfile.dispatchWindow(),
        integrationProfile.tribunalCodigo(),
        integrationProfile.tribunalNome(),
        integrationProfile.connectorSystem(),
        integrationProfile.competenceHint(),
        integrationProfile.connectorBaseUrl(),
        integrationProfile.connectorWorkflowMode(),
        integrationProfile.fallbackMode(),
        integrationProfile.contingencyDesk(),
        integrationProfile.replayQueue(),
        integrationProfile.evidenceRetentionPolicy(),
        integrationProfile.manualSubmissionDesk(),
        integrationProfile.telemetryMode(),
        integrationProfile.telemetryChannel(),
        integrationProfile.deadLetterQueue(),
        integrationProfile.reconciliationDesk(),
        integrationProfile.submissionAuditMode(),
        integrationProfile.protocolSlaBucket(),
        integrationProfile.escalationDesk(),
        integrationProfile.receiptAuditDesk(),
        integrationProfile.proofBundleMode(),
        integrationProfile.reconciliationWindow(),
        integrationProfile.stepUpRequired(),
        integrationProfile.certificateRequired(),
        integrationProfile.connectorWarnings(),
        summaryProjection.labels(),
        summaryProjection.metadata()
    );
  }

  public SecretariatQueuePanelSnapshotDto panel(String inboxKey, Collection<String> statuses, int limit) {
    SecretariatQueueInboxContext context = inboxContextResolver.resolve(inboxKey, statuses);
    List<SecretariatQueuePanelRow> rows = repo.listInbox(context.inboxKey(), context.statuses(), PageRequest.of(0, clampPanelLimit(limit)))
        .getContent()
        .stream()
        .map(this::toPanelRow)
        .toList();
    return panelProjectionSupport.buildSnapshot(context, rows);
  }

  public SecretariatQueueAgendaSnapshotDto agenda(String inboxKey, Collection<String> statuses, int daysPast, int daysFuture, int limit) {
    return agenda(inboxKey, statuses, daysPast, daysFuture, limit, SecretariatQueueAgendaFilter.empty());
  }

  public SecretariatQueueAgendaSnapshotDto agenda(String inboxKey,
                                                  Collection<String> statuses,
                                                  int daysPast,
                                                  int daysFuture,
                                                  int limit,
                                                  SecretariatQueueAgendaFilter filter) {
    SecretariatQueueInboxContext context = inboxContextResolver.resolve(inboxKey, statuses);
    Instant now = Instant.now();
    Instant from = now.minus(Math.max(0, daysPast), ChronoUnit.DAYS);
    Instant to = now.plus(Math.max(1, daysFuture), ChronoUnit.DAYS);
    SecretariatQueueAgendaFilter effectiveFilter = filter == null ? SecretariatQueueAgendaFilter.empty() : filter.normalized();
    List<SecretariatQueuePanelRow> rows = repo.findCalendarWindowByInboxKeys(List.of(context.inboxKey()), context.statuses(), from, to, PageRequest.of(0, clampAgendaLimit(limit)))
        .stream()
        .map(this::toPanelRow)
        .filter(row -> agendaProjectionSupport.matchesAgendaFilter(row, effectiveFilter))
        .toList();
    return agendaProjectionSupport.buildSnapshot(context, effectiveFilter, from, to, now, rows);
  }

  public String etagForInbox(String inboxKey, List<String> statuses, Pageable pageable) {
    Objects.requireNonNull(inboxKey, "inboxKey");
    Objects.requireNonNull(statuses, "statuses");
    Objects.requireNonNull(pageable, "pageable");

    String authorizedInboxKey = inboxContextResolver.requireAuthorizedInbox(inboxKey);
    List<String> normalizedStatuses = inboxContextResolver.normalizeStatuses(statuses);
    Object[] sig = repo.signature(authorizedInboxKey, normalizedStatuses);
    Instant maxUpdated = sig != null && sig.length > 0 ? (Instant) sig[0] : null;
    long count = sig != null && sig.length > 1 && sig[1] != null ? ((Number) sig[1]).longValue() : 0L;

    String material = authorizedInboxKey + "|" + String.join(",", normalizedStatuses)
        + "|p=" + pageable.getPageNumber() + "|s=" + pageable.getPageSize()
        + "|u=" + (maxUpdated == null ? "0" : maxUpdated.toEpochMilli())
        + "|c=" + count;

    return "W/\"" + sha256_16(material) + "\"";
  }

  public SecretariatGovernanceSnapshotDto governance(String inboxKey, Collection<String> statuses) {
    return governanceService.governance(inboxKey, statuses);
  }

  public SecretariatExceptionDeskSnapshotDto exceptions(String inboxKey, Collection<String> statuses) {
    return governanceService.exceptions(inboxKey, statuses);
  }

  public SecretariatCoverageSnapshotDto coverage(String inboxKey, Collection<String> statuses) {
    return governanceService.coverage(inboxKey, statuses);
  }

  public SecretariatFormalCatalogSnapshotDto formalCatalog(String inboxKey, Collection<String> statuses) {
    return governanceService.formalCatalog(inboxKey, statuses);
  }

  private SecretariatQueuePanelRow toPanelRow(SecretariatQueueItem item) {
    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(parseMetadata(item.getMetadataJson()));
    metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
    return panelRowProjectionSupport.toPanelRow(item, Collections.unmodifiableMap(metadata), parseTags(item.getTagsJson()));
  }

  private SecretariatQueueItemDto toDto(
      SecretariatQueueItem q,
      SecretariatQueueLoadProfile loadProfile,
      SecretariatDeskLoadProfile deskProfile,
      ForumDeskPortfolioProfile portfolio,
      String inboxDescriptor,
      String dashboardBucket,
      SecretariatInstitutionalVisibilityService.SecretariatInboxInstitutionalProfile inboxProfile,
      Processo processo
  ) {
    List<String> tags = parseTags(q.getTagsJson());
    SecretariatFlowBridgeProfile bridgeProfile = flowBridgeResolver.resolve(q.getInboxKey(), q.getQueueCode(), q.getTitulo(), tags, portfolio);
    SecretariatJudicialIntegrationProfile integrationProfile = integrationResolver.resolve(q.getInboxKey(), q.getQueueCode(), q.getTitulo(), tags, portfolio, bridgeProfile);
    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("updatedAt", q.getUpdatedAt());
    metadata.put("createdAt", q.getCreatedAt());
    metadata.put("pressureDescriptor", loadProfile.descriptor());
    metadata.put("deskLoadDescriptor", deskProfile.descriptor());
    metadata.put("bridgeDescriptor", bridgeProfile.descriptor());
    metadata.put("overdue", q.getDueAt() != null && q.getDueAt().isBefore(Instant.now()));
    metadata.put("highPriority", q.getPrioridade() != null && q.getPrioridade() <= 1);
    metadata.put("deskAxis", resolveDeskAxis(q.getQueueCode(), portfolio));
    metadata.put("complianceDesk", portfolio.complianceDesk());
    metadata.put("hearingDesk", portfolio.hearingDesk());
    metadata.put("escalationDesk", portfolio.escalationDesk());
    metadata.put("assistantDesk", portfolio.assistantDesk());
    metadata.put("coordinationDesk", portfolio.coordinationDesk());
    metadata.put("redistributionDesk", deskProfile.redistributionDesk());
    metadata.put("gabineteSupportDesk", deskProfile.gabineteSupportDesk());
    metadata.put("coordinationMode", deskProfile.coordinationMode());
    metadata.put("dueWindowHours", q.getDueAt() == null ? null : ChronoUnit.HOURS.between(Instant.now(), q.getDueAt()));
    metadata.putAll(bridgeProfile.toMap());
    metadata.putAll(integrationProfile.toMap());
    metadata.put("institutionalVisibility", inboxProfile.toMap());
    metadata.put("secretariatSpecialization", inboxProfile.specialization().toMap());
    SecretariatJudicialReferenceModelService.ReferenceModelSnapshot referenceSnapshot = referenceModelService.resolve(q.getInboxKey(), q.getQueueCode(), portfolio, deskProfile, bridgeProfile, integrationProfile);
    SecretariatInstitutionalAlignmentService.InstitutionalAlignmentSnapshot institutionalSnapshot = institutionalAlignmentService.resolve(q.getInboxKey(), q.getQueueCode(), inboxProfile.specialization(), bridgeProfile, integrationProfile);
    SecretariatOperationalDeskModelService.OperationalDeskSnapshot operationalDeskSnapshot = operationalDeskModelService.resolve(q.getInboxKey(), q.getQueueCode(), inboxProfile.specialization(), portfolio, deskProfile, bridgeProfile, integrationProfile);
    SecretariatOperationalActionModelService.OperationalActionSnapshot operationalActionSnapshot = operationalActionModelService.resolve(q.getInboxKey(), q.getQueueCode(), inboxProfile.specialization(), operationalDeskSnapshot, integrationProfile);
    SecretariatOperationalTransactionModelService.OperationalTransactionSnapshot operationalTransactionSnapshot = operationalTransactionModelService.resolve(operationalDeskSnapshot.journeyMode());
    SecretariatMigrationLaneService.MigrationLaneSnapshot migrationSnapshot = migrationLaneService.resolve(q.getInboxKey(), q.getQueueCode(), q.getTitulo(), tags, portfolio, bridgeProfile, integrationProfile);
    SecretariatHearingMediaLaneService.HearingMediaLaneSnapshot hearingMediaSnapshot = hearingMediaLaneService.resolve(q.getInboxKey(), q.getQueueCode(), q.getTitulo(), tags, portfolio, bridgeProfile, integrationProfile);
    metadata.put("referenceModels", referenceSnapshot.models());
    metadata.put("referenceGaps", referenceSnapshot.gaps());
    metadata.put("referenceDiagnostics", referenceSnapshot.diagnostics());
    metadata.put("institutionalCells", institutionalSnapshot.cells());
    metadata.put("institutionalTouchpoints", institutionalSnapshot.touchpoints());
    metadata.put("institutionalGaps", institutionalSnapshot.gaps());
    metadata.put("institutionalDiagnostics", institutionalSnapshot.diagnostics());
    metadata.put("operationalJourneyMode", operationalDeskSnapshot.journeyMode());
    metadata.put("operationalDesks", operationalDeskSnapshot.desks());
    metadata.put("operationalDeskGaps", operationalDeskSnapshot.gaps());
    metadata.put("operationalDeskDiagnostics", operationalDeskSnapshot.diagnostics());
    metadata.put("operationalActions", operationalActionSnapshot.actions());
    metadata.put("operationalActionGaps", operationalActionSnapshot.gaps());
    metadata.put("operationalActionDiagnostics", operationalActionSnapshot.diagnostics());
    metadata.put("operationalTransactions", operationalTransactionSnapshot.transactions());
    metadata.put("operationalTransactionDiagnostics", operationalTransactionSnapshot.diagnostics());
    metadata.put("migrationReadiness", migrationSnapshot.readiness());
    metadata.put("migrationDecision", migrationSnapshot.migrationDecision());
    metadata.put("migrationConnectorDecision", migrationSnapshot.connectorDecision());
    metadata.put("migrationTargetDesk", migrationSnapshot.targetDesk());
    metadata.put("migrationBlockers", migrationSnapshot.blockers());
    metadata.put("migrationSanitationActions", migrationSnapshot.sanitationActions());
    metadata.put("migrationAutomationOpportunities", migrationSnapshot.automationOpportunities());
    metadata.put("migrationDiagnostics", migrationSnapshot.diagnostics());
    metadata.put("hearingMediaTargetDesk", hearingMediaSnapshot.targetDesk());
    metadata.put("hearingMediaIndexingMode", hearingMediaSnapshot.indexingMode());
    metadata.put("hearingMediaAgendaReflection", hearingMediaSnapshot.agendaReflection());
    metadata.put("hearingMediaConnectorDecision", hearingMediaSnapshot.connectorMediaDecision());
    metadata.put("hearingMediaDiagnostics", hearingMediaSnapshot.diagnostics());
    metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

    return new SecretariatQueueItemDto(
        q.getWorkItemId(),
        q.getProcessoId(),
        q.getInboxKey(),
        q.getStatus(),
        q.getPrioridade(),
        q.getDueAt(),
        q.getScore(),
        tags,
        q.getTitulo(),
        processo == null ? null : firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso()),
        processo == null ? null : processo.getClasseProcessual(),
        processo == null || processo.getRito() == null ? null : processo.getRito().name(),
        processo == null ? null : resumoDoProcesso(processo),
        q.getQueueCode(),
        slaBucket(q.getDueAt()),
        inboxDescriptor,
        portfolio.operationalDescriptor(),
        dashboardBucket,
        loadProfile.loadBand(),
        loadProfile.responseMode(),
        bridgeProfile.downstreamAxis(),
        bridgeProfile.bridgeMode(),
        bridgeProfile.recursalDesk(),
        integrationProfile.targetSystem(),
        integrationProfile.protocolDesk(),
        integrationProfile.syncMode(),
        integrationProfile.reviewDesk(),
        integrationProfile.connectorId(),
        integrationProfile.ackChannel(),
        integrationProfile.replayDesk(),
        integrationProfile.retryMode(),
        integrationProfile.evidencePolicy(),
        integrationProfile.dispatchWindow(),
        integrationProfile.tribunalCodigo(),
        integrationProfile.tribunalNome(),
        integrationProfile.connectorSystem(),
        integrationProfile.competenceHint(),
        integrationProfile.connectorBaseUrl(),
        integrationProfile.connectorWorkflowMode(),
        integrationProfile.fallbackMode(),
        integrationProfile.contingencyDesk(),
        integrationProfile.replayQueue(),
        integrationProfile.evidenceRetentionPolicy(),
        integrationProfile.manualSubmissionDesk(),
        integrationProfile.telemetryMode(),
        integrationProfile.telemetryChannel(),
        integrationProfile.deadLetterQueue(),
        integrationProfile.reconciliationDesk(),
        integrationProfile.submissionAuditMode(),
        integrationProfile.protocolSlaBucket(),
        integrationProfile.escalationDesk(),
        integrationProfile.receiptAuditDesk(),
        integrationProfile.proofBundleMode(),
        integrationProfile.reconciliationWindow(),
        integrationProfile.stepUpRequired(),
        integrationProfile.certificateRequired(),
        integrationProfile.connectorWarnings(),
        mergeLabels(portfolio.labels(), deskProfile.labels(), bridgeProfile.labels(), integrationProfile.labels(), referenceSnapshot.labels(), institutionalSnapshot.labels(), operationalDeskSnapshot.labels(), operationalTransactionSnapshot.labels(), migrationSnapshot.labels(), hearingMediaSnapshot.labels()),
        Collections.unmodifiableMap(metadata),
        uiHints.hintsForSecretariatQueue(q)
    );
  }

  @SafeVarargs
  private static List<String> mergeLabels(List<String>... groups) {
    java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>();
    if (groups != null) {
      for (List<String> group : groups) {
        if (group != null) {
          merged.addAll(group);
        }
      }
    }
    return List.copyOf(merged);
  }

  private String resolveDeskAxis(String queueCode, ForumDeskPortfolioProfile portfolio) {
    String normalized = queueCode == null ? "" : queueCode.trim().toUpperCase(Locale.ROOT);
    if (normalized.contains("AUDI") || normalized.contains("HEARING")) {
      return firstNonBlank(portfolio.hearingDesk(), portfolio.triageDesk(), portfolio.executionDesk());
    }
    if (normalized.contains("GAB") || normalized.contains("DECISAO") || normalized.contains("CONCLUSAO")) {
      return firstNonBlank(portfolio.gabineteDesk(), portfolio.assistantDesk(), portfolio.triageDesk());
    }
    if (normalized.contains("ESCAL") || normalized.contains("INCIDENTE")) {
      return firstNonBlank(portfolio.escalationDesk(), portfolio.coordinationDesk(), portfolio.executionDesk());
    }
    return firstNonBlank(portfolio.complianceDesk(), portfolio.triageDesk(), portfolio.executionDesk(), portfolio.dashboardBucket());
  }

  private List<String> parseTags(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return mapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private Map<String, Object> parseMetadata(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ignored) {
      return Map.of();
    }
  }

  private static int clampPanelLimit(int limit) {
    if (limit <= 0) {
      return 120;
    }
    if (limit > 300) {
      return 300;
    }
    return limit;
  }

  private static int clampAgendaLimit(int limit) {
    if (limit <= 0) {
      return 180;
    }
    if (limit > 400) {
      return 400;
    }
    return limit;
  }

  private static String slaBucket(Instant dueAt) {
    if (dueAt == null) {
      return "NO_DUE";
    }
    Instant now = Instant.now();
    if (dueAt.isBefore(now)) {
      return "OVERDUE";
    }
    if (dueAt.isBefore(now.plus(2, ChronoUnit.HOURS))) {
      return "DUE_2H";
    }
    if (dueAt.isBefore(now.plus(24, ChronoUnit.HOURS))) {
      return "DUE_24H";
    }
    return "SCHEDULED";
  }

  static String resumoDoProcesso(Processo processo) {
    String resumoIA = processo.getResumoIA();
    if (resumoIA != null && !resumoIA.isBlank()) {
      return resumoIA.trim();
    }
    String classe = firstNonBlank(processo.getClasseProcessual());
    String assunto = firstNonBlank(processo.getAssunto());
    if (classe == null && assunto == null) {
      return null;
    }
    if (classe == null) {
      return assunto;
    }
    if (assunto == null) {
      return classe;
    }
    return classe + " — " + assunto;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private static String sha256_16(String material) {
    return Hashes.sha256HexPrefix(material, 32);
  }
}
