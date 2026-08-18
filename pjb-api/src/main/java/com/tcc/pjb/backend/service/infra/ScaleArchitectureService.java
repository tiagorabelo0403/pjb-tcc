package com.tcc.pjb.backend.service.infra;

import com.tcc.pjb.backend.configs.datasource.PjbAdaptiveDataPlaneService;
import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.configs.security.governance.ApiRouteGovernanceProperties;
import com.tcc.pjb.backend.core.jobs.runtime.JobDispatcherProperties;
import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRightsCoverageService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralTribunalVariationService;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfile;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleRuntimePolicyService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatInstitutionalAlignmentService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatJudicialReferenceModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalActionModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalDeskModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalTransactionModelService;
import com.tcc.pjb.backend.model.entity.infra.CachePolicyOverride;
import com.tcc.pjb.backend.model.entity.infra.PartitionPlan;
import com.tcc.pjb.backend.model.repository.CachePolicyOverrideRepository;
import com.tcc.pjb.backend.model.repository.PartitionPlanRepository;
import com.tcc.pjb.backend.model.repository.ProcessualReadModelMaterializationTrailRepository;
import com.tcc.pjb.backend.model.repository.ProcessualReadModelProjectionRepository;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.Year;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class ScaleArchitectureService {

    private final CachePolicyOverrideRepository cachePolicyOverrideRepository;
    private final PartitionPlanRepository partitionPlanRepository;
    private final ProcessualReadModelProjectionRepository processualReadModelProjectionRepository;
    private final ProcessualReadModelMaterializationTrailRepository processualReadModelMaterializationTrailRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<PjbAdaptiveDataPlaneService> adaptiveDataPlaneServiceProvider;
    private final ObjectProvider<PjbDataSourceRoutingProperties> dataSourceRoutingPropertiesProvider;
    private final ObjectProvider<PjbProcessualReadModelMeshService> processualReadModelMeshServiceProvider;
    private final ObjectProvider<PjbProcessualReadModelRecompositionQueueService> recompositionQueueServiceProvider;
    private final ObjectProvider<ApiRouteGovernanceProperties> apiRouteGovernancePropertiesProvider;
    private final ObjectProvider<JobDispatcherProperties> jobDispatcherPropertiesProvider;
    private final JudicialScaleProfileResolver judicialScaleProfileResolver;
    private final JudicialScaleRuntimePolicyService judicialScaleRuntimePolicyService;
    private final SecretariatJudicialReferenceModelService referenceModelService;
    private final SecretariatInstitutionalAlignmentService institutionalAlignmentService;
    private final SecretariatOperationalDeskModelService operationalDeskModelService;
    private final SecretariatOperationalActionModelService operationalActionModelService;
    private final SecretariatOperationalTransactionModelService operationalTransactionModelService;
    private final NationalProceduralRightsCoverageService proceduralRightsCoverageService;
    private final NationalProceduralOperationalPlaybookService proceduralOperationalPlaybookService;
    private final NationalProceduralTribunalVariationService proceduralTribunalVariationService;
    private final Environment environment;

    public ScaleArchitectureService(CachePolicyOverrideRepository cachePolicyOverrideRepository,
                                    PartitionPlanRepository partitionPlanRepository,
                                    ProcessualReadModelProjectionRepository processualReadModelProjectionRepository,
                                    ProcessualReadModelMaterializationTrailRepository processualReadModelMaterializationTrailRepository,
                                    JdbcTemplate jdbcTemplate,
                                    ObjectProvider<PjbAdaptiveDataPlaneService> adaptiveDataPlaneServiceProvider,
                                    ObjectProvider<PjbDataSourceRoutingProperties> dataSourceRoutingPropertiesProvider,
                                    ObjectProvider<PjbProcessualReadModelMeshService> processualReadModelMeshServiceProvider,
                                    ObjectProvider<PjbProcessualReadModelRecompositionQueueService> recompositionQueueServiceProvider,
                                    ObjectProvider<ApiRouteGovernanceProperties> apiRouteGovernancePropertiesProvider,
                                    ObjectProvider<JobDispatcherProperties> jobDispatcherPropertiesProvider,
                                    JudicialScaleProfileResolver judicialScaleProfileResolver,
                                    JudicialScaleRuntimePolicyService judicialScaleRuntimePolicyService,
                                    SecretariatJudicialReferenceModelService referenceModelService,
                                    SecretariatInstitutionalAlignmentService institutionalAlignmentService,
                                    SecretariatOperationalDeskModelService operationalDeskModelService,
                                    SecretariatOperationalActionModelService operationalActionModelService,
                                    SecretariatOperationalTransactionModelService operationalTransactionModelService,
                                    NationalProceduralRightsCoverageService proceduralRightsCoverageService,
                                    NationalProceduralOperationalPlaybookService proceduralOperationalPlaybookService,
                                    NationalProceduralTribunalVariationService proceduralTribunalVariationService,
                                    Environment environment) {
        this.cachePolicyOverrideRepository = Objects.requireNonNull(cachePolicyOverrideRepository);
        this.partitionPlanRepository = Objects.requireNonNull(partitionPlanRepository);
        this.processualReadModelProjectionRepository = Objects.requireNonNull(processualReadModelProjectionRepository);
        this.processualReadModelMaterializationTrailRepository = Objects.requireNonNull(processualReadModelMaterializationTrailRepository);
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.adaptiveDataPlaneServiceProvider = Objects.requireNonNull(adaptiveDataPlaneServiceProvider);
        this.dataSourceRoutingPropertiesProvider = Objects.requireNonNull(dataSourceRoutingPropertiesProvider);
        this.processualReadModelMeshServiceProvider = Objects.requireNonNull(processualReadModelMeshServiceProvider);
        this.recompositionQueueServiceProvider = Objects.requireNonNull(recompositionQueueServiceProvider);
        this.apiRouteGovernancePropertiesProvider = Objects.requireNonNull(apiRouteGovernancePropertiesProvider);
        this.jobDispatcherPropertiesProvider = Objects.requireNonNull(jobDispatcherPropertiesProvider);
        this.judicialScaleProfileResolver = Objects.requireNonNull(judicialScaleProfileResolver);
        this.judicialScaleRuntimePolicyService = Objects.requireNonNull(judicialScaleRuntimePolicyService);
        this.referenceModelService = Objects.requireNonNull(referenceModelService);
        this.institutionalAlignmentService = Objects.requireNonNull(institutionalAlignmentService);
        this.operationalDeskModelService = Objects.requireNonNull(operationalDeskModelService);
        this.operationalActionModelService = Objects.requireNonNull(operationalActionModelService);
        this.operationalTransactionModelService = Objects.requireNonNull(operationalTransactionModelService);
        this.proceduralRightsCoverageService = Objects.requireNonNull(proceduralRightsCoverageService);
        this.proceduralOperationalPlaybookService = Objects.requireNonNull(proceduralOperationalPlaybookService);
        this.proceduralTribunalVariationService = Objects.requireNonNull(proceduralTribunalVariationService);
        this.environment = Objects.requireNonNull(environment);
    }

    @Transactional(readOnly = true)
    public List<CachePolicyView> listarPoliticasCache() {
        List<CachePolicyView> overrides = cachePolicyOverrideRepository.findByEnabledTrueOrderByCacheNameAscRoleNameAsc().stream()
                .map(entity -> new CachePolicyView(entity.getId(), entity.getCacheName(), entity.getRoleName(), entity.getTtlSeconds(),
                        entity.getStaleWhileRevalidateSeconds(), entity.isEnabled(), entity.getNotes(), "OVERRIDE"))
                .toList();
        if (!overrides.isEmpty()) {
            return overrides;
        }
        return defaults().entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    return new CachePolicyView(null, parts[0], parts[1], entry.getValue().ttlSeconds(), entry.getValue().staleWhileRevalidateSeconds(), true, entry.getValue().notes(), "DEFAULT");
                })
                .toList();
    }

    @Transactional
    public CachePolicyView salvarPoliticaCache(CachePolicyRequest request) {
        CachePolicyOverride entity = cachePolicyOverrideRepository
                .findByCacheNameIgnoreCaseAndRoleNameIgnoreCase(request.cacheName(), request.roleName())
                .orElseGet(CachePolicyOverride::new);
        entity.setCacheName(normalize(request.cacheName()));
        entity.setRoleName(normalize(request.roleName()));
        entity.setTtlSeconds(Math.max(1, request.ttlSeconds()));
        entity.setStaleWhileRevalidateSeconds(Math.max(0, request.staleWhileRevalidateSeconds()));
        entity.setEnabled(request.enabled());
        entity.setNotes(request.notes());
        CachePolicyOverride saved = cachePolicyOverrideRepository.save(entity);
        return new CachePolicyView(saved.getId(), saved.getCacheName(), saved.getRoleName(), saved.getTtlSeconds(),
                saved.getStaleWhileRevalidateSeconds(), saved.isEnabled(), saved.getNotes(), "OVERRIDE");
    }

    @PjbTransactionalBudget(operation = "infra.scale-architecture.listar-planos-particao", maxMillis = 3000)
    @Transactional(readOnly = true)
    public List<PartitionPlanView> listarPlanosParticao() {
        return partitionPlanRepository.findAll().stream()
                .map(plan -> new PartitionPlanView(plan.getId(), plan.getTableName(), plan.getPartitionColumn(), plan.getPartitionPrefix(),
                        plan.getStartYear(), plan.getYearsAhead(), plan.getStatus(), plan.getLastMaterializedYear(), plan.getNotes()))
                .toList();
    }


    @Transactional(readOnly = true)
    public JudicialScaleGovernanceView judicialScaleProfilesView() {
        PjbDataSourceRoutingProperties routing = dataSourceRoutingPropertiesProvider.getIfAvailable();
        ApiRouteGovernanceProperties routeGovernance = apiRouteGovernancePropertiesProvider.getIfAvailable();
        JobDispatcherProperties jobDispatcher = jobDispatcherPropertiesProvider.getIfAvailable();
        PjbDataSourceRoutingProperties.AdaptivePlane adaptivePlane = routing == null ? new PjbDataSourceRoutingProperties.AdaptivePlane() : routing.getAdaptivePlane();
        JobDispatcherProperties effectiveJobs = jobDispatcher == null ? new JobDispatcherProperties() : jobDispatcher;
        List<JudicialScaleProfileView> profiles = java.util.Arrays.stream(JudicialScaleProfile.values())
                .map(profile -> new JudicialScaleProfileView(
                        profile.name(),
                        profile.displayName(),
                        profile.instanceClass(),
                        profile.queueParallelismFactor(),
                        profile.queueBudgetFactor(),
                        profile.replicaLagFactor(),
                        profile.readPressureFactor(),
                        profile.rateLimitFactor(),
                        profile.cacheHotPreferred(),
                        profile.searchIndexPreferred(),
                        profile.asyncWritePreferred()
                ))
                .toList();
        List<JudicialScaleMatrixRowView> matrix = judicialScaleProfileResolver.defaultMatrix().stream()
                .map(policy -> new JudicialScaleMatrixRowView(
                        policy.profile().name(),
                        policy.displayName(),
                        policy.instanceClass(),
                        policy.branchClass(),
                        policy.queueParallelismFactor(),
                        policy.queueBudgetFactor(),
                        Math.max(1, Math.min(effectiveJobs.getMaxParallel(), (int) Math.round(effectiveJobs.getMaxParallel() * policy.queueParallelismFactor()))),
                        Math.max(1, Math.min(effectiveJobs.getMaxParallelPerType(), (int) Math.round(effectiveJobs.getMaxParallelPerType() * policy.queueParallelismFactor()))),
                        effectiveJobs.budgetMillisForType("*", policy.queueBudgetFactor()),
                        adaptiveReadThreshold(adaptivePlane, policy),
                        Math.max(1, (int) Math.round(adaptivePlane.getReadPoolAwaitingThreshold() * policy.readPressureFactor())),
                        judicialScaleProfileResolver.scaleDuration(adaptivePlane.getReplicaLagTolerance(), policy.replicaLagFactor()),
                        judicialScaleProfileResolver.scaleDuration(adaptivePlane.getDegradedReplicaLagTolerance(), policy.degradedReplicaLagFactor()),
                        policy.cacheHotPreferred(),
                        policy.searchPreferred(),
                        policy.asyncWritePreferred(),
                        effectiveRatePreviews(routeGovernance, policy),
                        policy.metadata()
                ))
                .toList();
        return new JudicialScaleGovernanceView(
                routing != null && routing.isEnabled(),
                profiles,
                matrix
        );
    }

    private double adaptiveReadThreshold(PjbDataSourceRoutingProperties.AdaptivePlane adaptivePlane,
                                         JudicialScaleProfileResolver.JudicialScalePolicy policy) {
        return Math.max(0.35d, Math.min(0.99d, adaptivePlane.getReadPoolActiveRatioThreshold() * policy.readPressureFactor()));
    }

    private Map<String, Long> effectiveRatePreviews(ApiRouteGovernanceProperties routeGovernance,
                                                    JudicialScaleProfileResolver.JudicialScalePolicy policy) {
        if (routeGovernance == null || routeGovernance.getRules().isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        for (ApiRouteGovernanceProperties.Rule rule : routeGovernance.getRules()) {
            if (rule == null || rule.getName() == null || rule.getName().isBlank() || rule.getMaxRequestsPerWindow() <= 0L) {
                continue;
            }
            if (!isLoadBearingRule(rule.getName())) {
                continue;
            }
            out.put(rule.getName(), Math.max(1L, Math.round(rule.getMaxRequestsPerWindow() * policy.rateLimitFactor())));
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private boolean isLoadBearingRule(String ruleName) {
        String normalized = ruleName == null ? "" : ruleName.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("all-api")
                || normalized.equals("admin-governance")
                || normalized.equals("gabinete-juiz")
                || normalized.equals("secretaria-operacional");
    }

    @Transactional(readOnly = true)
    public JudicialRuntimePolicyGovernanceView judicialRuntimePoliciesView() {
        List<JudicialScaleRuntimePolicyService.JudicialRuntimePolicyView> policies = judicialScaleProfileResolver.defaultMatrix().stream()
                .map(policy -> judicialScaleRuntimePolicyService.preview(policy.instanceClass(), policy.branchClass()))
                .toList();
        return new JudicialRuntimePolicyGovernanceView(policies);
    }

    @Transactional(readOnly = true)
    public JudicialSecretariatModelGovernanceView judicialSecretariatModelsView() {
        SecretariatJudicialReferenceModelService.ReferenceCatalogView catalog = referenceModelService.catalog();
        List<JudicialSecretariatModelRowView> rows = catalog.rows().stream()
                .map(row -> new JudicialSecretariatModelRowView(
                        row.instanceClass(),
                        row.branchClass(),
                        row.descriptor(),
                        row.queueFamilies(),
                        row.capabilities()
                ))
                .toList();
        return new JudicialSecretariatModelGovernanceView(rows);
    }


    @Transactional(readOnly = true)
    public JudicialOperationalDeskGovernanceView judicialOperationalDesksView() {
        SecretariatOperationalDeskModelService.OperationalDeskCatalogView catalog = operationalDeskModelService.catalog();
        List<JudicialOperationalDeskRowView> rows = catalog.rows().stream()
                .map(row -> new JudicialOperationalDeskRowView(
                        row.journeyMode(),
                        row.descriptor(),
                        row.desks()
                ))
                .toList();
        return new JudicialOperationalDeskGovernanceView(rows);
    }


    @Transactional(readOnly = true)
    public JudicialOperationalActionGovernanceView judicialOperationalActionsView() {
        SecretariatOperationalActionModelService.OperationalActionCatalogView catalog = operationalActionModelService.catalog();
        List<JudicialOperationalActionRowView> rows = catalog.rows().stream()
                .map(row -> new JudicialOperationalActionRowView(
                        row.journeyMode(),
                        row.descriptor(),
                        row.actions()
                ))
                .toList();
        return new JudicialOperationalActionGovernanceView(rows);
    }


    @Transactional(readOnly = true)
    public JudicialOperationalTransactionGovernanceView judicialOperationalTransactionsView() {
        SecretariatOperationalTransactionModelService.OperationalTransactionCatalogView catalog = operationalTransactionModelService.catalog();
        List<JudicialOperationalTransactionRowView> rows = catalog.rows().stream()
                .map(row -> new JudicialOperationalTransactionRowView(
                        row.journeyMode(),
                        row.descriptor(),
                        row.transactions()
                ))
                .toList();
        return new JudicialOperationalTransactionGovernanceView(rows);
    }

    @Transactional(readOnly = true)
    public Object judicialProceduralCoverageView() {
        return proceduralRightsCoverageService.snapshot();
    }

    @Transactional(readOnly = true)
    public Object judicialProceduralCoverageDetailView(String rito) {
        return proceduralRightsCoverageService.describe(rito);
    }


    @Transactional(readOnly = true)
    public Object judicialProceduralPlaybookView() {
        return proceduralOperationalPlaybookService.snapshot();
    }

    @Transactional(readOnly = true)
    public Object judicialProceduralPlaybookDetailView(String rito) {
        return proceduralOperationalPlaybookService.describe(rito);
    }

    @Transactional(readOnly = true)
    public Object judicialTribunalVariationView() {
        return proceduralTribunalVariationService.snapshot();
    }

    @Transactional(readOnly = true)
    public Object judicialTribunalVariationDetailView(String tribunalCodigo, String rito, String unidadeCodigo, String tipoJustica) {
        return proceduralTribunalVariationService.describe(tribunalCodigo, unidadeCodigo, rito, tipoJustica);
    }

    @Transactional(readOnly = true)
    public JudicialInstitutionalAlignmentGovernanceView judicialInstitutionalAlignmentView() {
        SecretariatInstitutionalAlignmentService.InstitutionalCatalogView catalog = institutionalAlignmentService.catalog();
        List<JudicialInstitutionalAlignmentRowView> rows = catalog.rows().stream()
                .map(row -> new JudicialInstitutionalAlignmentRowView(
                        row.institutionalAxis(),
                        row.descriptor(),
                        row.cells(),
                        row.touchpoints()
                ))
                .toList();
        return new JudicialInstitutionalAlignmentGovernanceView(rows);
    }

    @Transactional(readOnly = true)
    public AdaptiveDataPlaneView adaptiveDataPlaneView() {
        PjbAdaptiveDataPlaneService adaptiveDataPlaneService = adaptiveDataPlaneServiceProvider.getIfAvailable();
        PjbAdaptiveDataPlaneService.AdaptiveBlueprint blueprint = adaptiveDataPlaneService == null ? null : adaptiveDataPlaneService.blueprint();
        if (blueprint == null) {
            return new AdaptiveDataPlaneView(false, Duration.ZERO, Duration.ZERO, 0d, 0d, 0, 0, List.of(), List.of(), List.of(), List.of(), false, List.of(), null, List.of());
        }
        List<AdaptiveLaneView> lanes = blueprint.accelerationLanes().stream()
                .map(lane -> new AdaptiveLaneView(lane.mode(), lane.target(), lane.rationale()))
                .toList();
        return new AdaptiveDataPlaneView(
                blueprint.enabled(),
                blueprint.replicaLagTolerance(),
                blueprint.degradedReplicaLagTolerance(),
                blueprint.readPoolActiveRatioThreshold(),
                blueprint.writePoolActiveRatioThreshold(),
                blueprint.readPoolAwaitingThreshold(),
                blueprint.writePoolAwaitingThreshold(),
                blueprint.primaryCriticalPrefixes(),
                blueprint.hotCachePrefixes(),
                blueprint.searchBackedPrefixes(),
                blueprint.asyncWritePrefixes(),
                blueprint.sovereignFallbackEnabled(),
                blueprint.sovereignFallbackScopes(),
                blueprint.processualReadModelsTopic(),
                lanes
        );
    }

    @Transactional(readOnly = true)
    public DatabaseRuntimePostureView databaseRuntimePostureView() {
        PjbDataSourceRoutingProperties routing = dataSourceRoutingPropertiesProvider.getIfAvailable();
        boolean readRoutingEnabled = routing != null && routing.isEnabled();
        boolean strictReplicaRouting = routing != null && routing.isStrict();
        boolean adaptivePlaneEnabled = routing != null && routing.getAdaptivePlane().isEnabled();
        boolean sovereignFallbackEnabled = routing != null && routing.getSovereignFallback().isEnabled();
        boolean writeFailoverEnabled = routing != null && routing.getWriteFailover().isEnabled();
        boolean writeFailoverStrict = routing != null && routing.getWriteFailover().isStrict();
        List<String> regionalReplicas = routing == null ? List.of() : routing.getRegionalReplicas().keySet().stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .sorted()
                .toList();
        List<String> writeFailoverCandidates = routing == null ? List.of() : routing.getWriteFailover().getCandidates().keySet().stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .sorted()
                .toList();
        Integer writePoolMax = environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class);
        Integer writePoolMin = environment.getProperty("spring.datasource.hikari.minimum-idle", Integer.class);
        Long keepaliveTime = environment.getProperty("spring.datasource.hikari.keepalive-time", Long.class);
        boolean rewriteBatchedInserts = environment.getProperty("spring.datasource.hikari.data-source-properties.reWriteBatchedInserts", Boolean.class, Boolean.TRUE);
        boolean binaryTransfer = environment.getProperty("spring.datasource.hikari.data-source-properties.binaryTransfer", Boolean.class, Boolean.TRUE);
        boolean tcpKeepAlive = environment.getProperty("spring.datasource.hikari.data-source-properties.tcpKeepAlive", Boolean.class, Boolean.TRUE);
        String writeUrl = environment.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/pjb");
        String readUrl = firstNonBlank(environment.getProperty("pjb.datasource.routing.replica.url"), environment.getProperty("PJB_DB_READ_URL"));
        String loadBalancerMode = environment.getProperty("PJB_DB_LOAD_BALANCER_MODE", "DB_EDGE_TCP");
        return new DatabaseRuntimePostureView(
                "POSTGRESQL_18",
                "HORIZONTAL_PRIMARY_WITH_VERTICAL_ELASTICITY",
                true,
                true,
                readRoutingEnabled,
                strictReplicaRouting,
                adaptivePlaneEnabled,
                sovereignFallbackEnabled,
                writeFailoverEnabled,
                writeFailoverStrict,
                true,
                true,
                true,
                true,
                true,
                true,
                loadBalancerMode,
                maskJdbcUrl(writeUrl),
                maskJdbcUrl(readUrl),
                new PoolTopologyView(writePoolMax, writePoolMin, keepaliveTime != null && keepaliveTime > 0, rewriteBatchedInserts, binaryTransfer, tcpKeepAlive),
                new ReplicaPoolTopologyView(
                        routing == null ? null : routing.getReplica().getHikari().getMaximumPoolSize(),
                        routing == null ? null : routing.getReplica().getHikari().getMinimumIdle(),
                        routing != null && routing.getReplica().isFallbackToWriteOnError(),
                        routing == null ? null : routing.getReplica().getFailureCooldown(),
                        regionalReplicas
                ),
                List.of("tb_work_item", "tb_outbox_event", "tb_processo_event", "tb_ui_state_history", "notification_history"),
                List.of("tb_outbox_event", "tb_processo_event", "tb_ui_state_history", "notification_history"),
                List.of("REDIS_HOT_CACHE", "CAFFEINE_LOCAL_HOT_CACHE"),
                List.of("KAFKA_DEFERRED_QUEUE", "OUTBOX_EVENT_STREAM", "READ_MODEL_RECOMPOSITION_QUEUE"),
                List.of("PGBOUNCER_TRANSACTION_POOLING", "DB_EDGE_TCP_LOAD_BALANCER", "ADAPTIVE_DATA_PLANE_FILTER", "DB_PRESSURE_SHIELD", "STRICT_READ_REPLICA_ROUTING", "RLS_READY_TABLE_POLICY"),
                writeFailoverCandidates
        );
    }

    @Transactional(readOnly = true)
    public ProcessualReadModelsView processualReadModelsView() {
        PjbProcessualReadModelMeshService service = processualReadModelMeshServiceProvider.getIfAvailable();
        PjbProcessualReadModelMeshService.ProcessualReadModelBlueprint blueprint = service == null ? null : service.blueprint();
        if (blueprint == null) {
            return new ProcessualReadModelsView(false, null, List.of());
        }
        List<ProcessualDomainView> domains = blueprint.domains().stream()
                .map(domain -> new ProcessualDomainView(
                        domain.domain(),
                        domain.consistencyMode(),
                        domain.sovereignScope(),
                        domain.hotPath(),
                        domain.routePrefixes(),
                        domain.cacheNames(),
                        domain.eventFragments(),
                        domain.freshness() == null ? null : new ProjectionFreshnessView(
                                domain.freshness().updatedAt(),
                                domain.freshness().lastEventType(),
                                domain.freshness().lastAggregateType(),
                                domain.freshness().lastAggregateId(),
                                domain.freshness().source()
                        )
                ))
                .toList();
        return new ProcessualReadModelsView(blueprint.enabled(), blueprint.kafkaTopic(), domains);
    }

    @Transactional(readOnly = true)
    public ProcessualReadModelPersistenceView processualReadModelPersistenceView() {
        PjbProcessualReadModelRecompositionQueueService queueService = recompositionQueueServiceProvider.getIfAvailable();
        PjbProcessualReadModelRecompositionQueueService.QueueSnapshot queueSnapshot = queueService == null ? new PjbProcessualReadModelRecompositionQueueService.QueueSnapshot(0, 0, 0, 0, List.of()) : queueService.snapshot();
        List<ProjectionMaterializationView> projections = processualReadModelProjectionRepository.findTop20ByOrderByUpdatedAtDesc().stream()
                .map(projection -> new ProjectionMaterializationView(
                        projection.getId(),
                        projection.getDomain(),
                        projection.getMaterializationKey(),
                        projection.getScopeKey(),
                        projection.getAggregateType(),
                        projection.getAggregateId(),
                        projection.getTribunalCode(),
                        projection.getRamoCode(),
                        projection.getProjectionVersion(),
                        projection.getLastEventType(),
                        projection.getSource(),
                        projection.getStatus(),
                        projection.getFreshnessAt(),
                        projection.getLastMaterializedAt(),
                        projection.getLastRecompositionRequestedAt(),
                        projection.getUpdatedAt()
                ))
                .toList();
        List<MaterializationTrailView> trails = processualReadModelMaterializationTrailRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(trail -> new MaterializationTrailView(
                        trail.getId(),
                        trail.getProjectionDomain(),
                        trail.getProjectionKey(),
                        trail.getProjectionVersion(),
                        trail.getPreviousVersion(),
                        trail.getEventType(),
                        trail.getAggregateType(),
                        trail.getAggregateId(),
                        trail.getTribunalCode(),
                        trail.getRamoCode(),
                        trail.getSource(),
                        trail.getMaterializationHash(),
                        trail.getMaterializationStatus(),
                        trail.getOccurredAt(),
                        trail.getCreatedAt(),
                        trail.getNotes()
                ))
                .toList();
        return new ProcessualReadModelPersistenceView(
                true,
                processualReadModelProjectionRepository.count(),
                queueSnapshot.pending(),
                queueSnapshot.processing(),
                queueSnapshot.failed(),
                queueSnapshot.completed(),
                projections,
                trails,
                queueSnapshot.jobs()
        );
    }

    @Transactional
    public PjbProcessualReadModelRecompositionQueueService.JobView enqueueProcessualReadModelRecomposition(ProcessualReadModelRecompositionRequest request) {
        PjbProcessualReadModelRecompositionQueueService queueService = recompositionQueueServiceProvider.getIfAvailable();
        if (queueService == null) {
            throw new IllegalStateException("Fila de recomposicao processual indisponivel.");
        }
        return queueService.enqueue(
                request.domain(),
                request.tribunalCode(),
                request.ramoCode(),
                request.scopeKey(),
                request.requestedBy(),
                request.reason()
        );
    }

    @Transactional
    public PartitionPlanView salvarPlanoParticao(PartitionPlanRequest request) {
        PartitionPlan plan = partitionPlanRepository.findByTableNameIgnoreCase(request.tableName())
                .orElseGet(PartitionPlan::new);
        plan.setTableName(normalizeTable(request.tableName()));
        plan.setPartitionColumn(normalize(request.partitionColumn()));
        plan.setPartitionPrefix(normalizeTable(request.partitionPrefix()));
        plan.setStartYear(Math.max(2020, request.startYear()));
        plan.setYearsAhead(Math.max(1, request.yearsAhead()));
        plan.setStatus("ATIVO");
        plan.setNotes(request.notes());
        PartitionPlan saved = partitionPlanRepository.save(plan);
        return new PartitionPlanView(saved.getId(), saved.getTableName(), saved.getPartitionColumn(), saved.getPartitionPrefix(),
                saved.getStartYear(), saved.getYearsAhead(), saved.getStatus(), saved.getLastMaterializedYear(), saved.getNotes());
    }

    @Transactional(readOnly = true)
    public PartitionPreview previewMaterializacao(String tableName) {
        PartitionPlan plan = partitionPlanRepository.findByTableNameIgnoreCase(tableName)
                .orElseThrow(() -> new IllegalArgumentException("Plano de particionamento nao encontrado."));
        int currentYear = Year.now().getValue();
        int initialYear = Math.max(plan.getStartYear(), currentYear);
        int lastYear = Math.max(initialYear, currentYear + Math.max(1, plan.getYearsAhead()));
        List<String> ddls = java.util.stream.IntStream.rangeClosed(initialYear, lastYear)
                .mapToObj(year -> buildPartitionDdl(plan, year))
                .toList();
        return new PartitionPreview(plan.getTableName(), initialYear, lastYear, ddls);
    }

    @Transactional
    public PartitionPreview materializar(String tableName) {
        PartitionPreview preview = previewMaterializacao(tableName);
        for (String ddl : preview.ddlStatements()) {
            jdbcTemplate.execute(ddl);
        }
        PartitionPlan plan = partitionPlanRepository.findByTableNameIgnoreCase(tableName)
                .orElseThrow(() -> new IllegalArgumentException("Plano de particionamento nao encontrado."));
        plan.setLastMaterializedYear(preview.endYear());
        partitionPlanRepository.save(plan);
        return preview;
    }

    private String buildPartitionDdl(PartitionPlan plan, int year) {
        String shardTable = normalizeTable(plan.getPartitionPrefix()) + "_" + year;
        return "CREATE TABLE IF NOT EXISTS " + shardTable +
                " (LIKE " + normalizeTable(plan.getTableName()) + " INCLUDING ALL);";
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTable(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        while (normalized.contains("__")) {
            normalized = normalized.replace("__", "_");
        }
        return normalized;
    }

    private String maskJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "NAO_CONFIGURADO";
        }
        int protocolIndex = jdbcUrl.indexOf("://");
        if (protocolIndex < 0) {
            return jdbcUrl;
        }
        int pathIndex = jdbcUrl.indexOf('/', protocolIndex + 3);
        if (pathIndex < 0) {
            return jdbcUrl;
        }
        int queryIndex = jdbcUrl.indexOf('?', pathIndex);
        String head = jdbcUrl.substring(0, pathIndex);
        String database = queryIndex < 0 ? jdbcUrl.substring(pathIndex + 1) : jdbcUrl.substring(pathIndex + 1, queryIndex);
        return head + "/" + database;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private Map<String, DefaultCachePolicy> defaults() {
        Map<String, DefaultCachePolicy> out = new LinkedHashMap<>();
        out.put("judge_dashboard|JUIZ", new DefaultCachePolicy(15, 5, "Painel decisional com refresh curto."));
        out.put("public_timeline|CIDADAO", new DefaultCachePolicy(60, 20, "Linha do tempo publica de alto volume."));
        out.put("laiane_judge|MAGISTRADO", new DefaultCachePolicy(20, 10, "IA magistratura com janela curta de reuso."));
        out.put("processo_resumo|ADVOGADO", new DefaultCachePolicy(45, 15, "Resumo processual para advocacia."));
        out.put("camara_governanca|DESEMBARGADOR", new DefaultCachePolicy(30, 10, "Painel colegiado com giro medio."));
        out.put("plenario_publico|MINISTRO", new DefaultCachePolicy(25, 10, "Publicacao de sessao plenaria."));
        out.put("processo_timeline_hot|ADVOGADO", new DefaultCachePolicy(20, 8, "Timeline resumida de processo com invalidação por evento."));
        out.put("audiencia_agenda|SERVIDOR", new DefaultCachePolicy(30, 10, "Agenda forense e mapa de audiencias."));
        out.put("peticionamento_workspace|ADVOGADO", new DefaultCachePolicy(12, 4, "Workspace de peticionamento com leitura quente e protecao de consistencia."));
        return out;
    }

    private record DefaultCachePolicy(int ttlSeconds, int staleWhileRevalidateSeconds, String notes) {
    }

    public record CachePolicyRequest(
            @NotBlank String cacheName,
            @NotBlank String roleName,
            @Min(1) int ttlSeconds,
            @Min(0) int staleWhileRevalidateSeconds,
            boolean enabled,
            String notes
    ) {
    }

    public record CachePolicyView(
            Long id,
            String cacheName,
            String roleName,
            Integer ttlSeconds,
            Integer staleWhileRevalidateSeconds,
            boolean enabled,
            String notes,
            String source
    ) {
    }

    public record JudicialSecretariatModelGovernanceView(
            List<JudicialSecretariatModelRowView> rows
    ) {
    }

    public record JudicialSecretariatModelRowView(
            String instanceClass,
            String branchClass,
            String descriptor,
            List<String> queueFamilies,
            Map<String, Object> capabilities
    ) {
    }


    public record JudicialOperationalDeskGovernanceView(
            List<JudicialOperationalDeskRowView> rows
    ) {
    }

    public record JudicialOperationalDeskRowView(
            String journeyMode,
            String descriptor,
            List<SecretariatOperationalDeskModelService.OperationalDeskView> desks
    ) {
    }


    public record JudicialOperationalActionGovernanceView(
            List<JudicialOperationalActionRowView> rows
    ) {
    }

    public record JudicialOperationalActionRowView(
            String journeyMode,
            String descriptor,
            List<SecretariatOperationalActionModelService.OperationalDeskActionView> actions
    ) {
    }

    public record JudicialOperationalTransactionGovernanceView(
            List<JudicialOperationalTransactionRowView> rows
    ) {
    }

    public record JudicialOperationalTransactionRowView(
            String journeyMode,
            String descriptor,
            List<SecretariatOperationalTransactionModelService.OperationalTransactionView> transactions
    ) {
    }

    public record JudicialInstitutionalAlignmentGovernanceView(
            List<JudicialInstitutionalAlignmentRowView> rows
    ) {
    }

    public record JudicialInstitutionalAlignmentRowView(
            String institutionalAxis,
            String descriptor,
            List<String> cells,
            List<String> touchpoints
    ) {
    }

    public record AdaptiveDataPlaneView(
            boolean enabled,
            Duration replicaLagTolerance,
            Duration degradedReplicaLagTolerance,
            double readPoolActiveRatioThreshold,
            double writePoolActiveRatioThreshold,
            int readPoolAwaitingThreshold,
            int writePoolAwaitingThreshold,
            List<String> primaryCriticalPrefixes,
            List<String> hotCachePrefixes,
            List<String> searchBackedPrefixes,
            List<String> asyncWritePrefixes,
            boolean sovereignFallbackEnabled,
            List<String> sovereignFallbackScopes,
            String processualReadModelsTopic,
            List<AdaptiveLaneView> lanes
    ) {
    }

    public record JudicialScaleGovernanceView(
            boolean adaptiveRuntimeEnabled,
            List<JudicialScaleProfileView> profiles,
            List<JudicialScaleMatrixRowView> matrix
    ) {
    }

    public record JudicialScaleProfileView(
            String profileCode,
            String displayName,
            String instanceClass,
            double queueParallelismFactor,
            double queueBudgetFactor,
            double replicaLagFactor,
            double readPressureFactor,
            double rateLimitFactor,
            boolean cacheHotPreferred,
            boolean searchPreferred,
            boolean asyncWritePreferred
    ) {
    }

    public record JudicialScaleMatrixRowView(
            String profileCode,
            String displayName,
            String instanceClass,
            String branchClass,
            double queueParallelismFactor,
            double queueBudgetFactor,
            int queueMaxParallelPreview,
            int queuePerTypePreview,
            long queueBudgetMillisPreview,
            double adaptiveReadPressureThreshold,
            int adaptiveReadAwaitingThreshold,
            Duration replicaLagTolerance,
            Duration degradedReplicaLagTolerance,
            boolean cacheHotPreferred,
            boolean searchPreferred,
            boolean asyncWritePreferred,
            Map<String, Long> effectiveRouteRateLimits,
            Map<String, Object> metadata
    ) {
    }

    public record JudicialRuntimePolicyGovernanceView(
            List<JudicialScaleRuntimePolicyService.JudicialRuntimePolicyView> policies
    ) {
    }

    public record ProcessualReadModelsView(
            boolean enabled,
            String kafkaTopic,
            List<ProcessualDomainView> domains
    ) {
    }

    public record ProcessualReadModelPersistenceView(
            boolean persistenceEnabled,
            long projectionCount,
            long pendingRecompositionCount,
            long processingRecompositionCount,
            long failedRecompositionCount,
            long completedRecompositionCount,
            List<ProjectionMaterializationView> projections,
            List<MaterializationTrailView> trails,
            List<PjbProcessualReadModelRecompositionQueueService.JobView> recompositionJobs
    ) {
    }

    public record ProcessualDomainView(
            String domain,
            String consistencyMode,
            String sovereignScope,
            boolean hotPath,
            List<String> routePrefixes,
            List<String> cacheNames,
            List<String> eventFragments,
            ProjectionFreshnessView freshness
    ) {
    }

    public record ProjectionFreshnessView(
            java.time.Instant updatedAt,
            String lastEventType,
            String lastAggregateType,
            String lastAggregateId,
            String source
    ) {
    }

    public record ProjectionMaterializationView(
            Long id,
            String domain,
            String materializationKey,
            String scopeKey,
            String aggregateType,
            String aggregateId,
            String tribunalCode,
            String ramoCode,
            Long version,
            String lastEventType,
            String source,
            String status,
            java.time.Instant freshnessAt,
            java.time.Instant lastMaterializedAt,
            java.time.Instant lastRecompositionRequestedAt,
            java.time.Instant updatedAt
    ) {
    }

    public record MaterializationTrailView(
            Long id,
            String projectionDomain,
            String projectionKey,
            Long projectionVersion,
            Long previousVersion,
            String eventType,
            String aggregateType,
            String aggregateId,
            String tribunalCode,
            String ramoCode,
            String source,
            String materializationHash,
            String materializationStatus,
            java.time.Instant occurredAt,
            java.time.Instant createdAt,
            String notes
    ) {
    }

    public record AdaptiveLaneView(
            String mode,
            String target,
            String rationale
    ) {
    }

    public record DatabaseRuntimePostureView(
            String engineTarget,
            String scalingMode,
            boolean horizontalPrimaryFirst,
            boolean verticalElasticityEnabled,
            boolean readRoutingEnabled,
            boolean strictReplicaRouting,
            boolean adaptiveDataPlaneEnabled,
            boolean sovereignFallbackEnabled,
            boolean writeFailoverEnabled,
            boolean writeFailoverStrict,
            boolean pgStatStatementsRequired,
            boolean trackIoTimingRequired,
            boolean partitionGovernanceEnabled,
            boolean connectionProxyExpected,
            boolean loadBalancerExpected,
            boolean rowLevelSecurityReady,
            String loadBalancerMode,
            String writeJdbcEntryPoint,
            String readJdbcEntryPoint,
            PoolTopologyView writePool,
            ReplicaPoolTopologyView readPool,
            List<String> hotPartitionTables,
            List<String> retentionPriorityTables,
            List<String> cachePlanes,
            List<String> queuePlanes,
            List<String> securityLayers,
            List<String> writeFailoverCandidates
    ) {
    }

    public record PoolTopologyView(
            Integer maximumPoolSize,
            Integer minimumIdle,
            boolean keepaliveConfigured,
            boolean rewriteBatchedInserts,
            boolean binaryTransferEnabled,
            boolean tcpKeepAliveEnabled
    ) {
    }

    public record ReplicaPoolTopologyView(
            Integer maximumPoolSize,
            Integer minimumIdle,
            boolean fallbackToWriteOnError,
            Duration failureCooldown,
            List<String> regionalReplicas
    ) {
    }

    public record ProcessualReadModelRecompositionRequest(
            @NotBlank String domain,
            String tribunalCode,
            String ramoCode,
            String scopeKey,
            String requestedBy,
            String reason
    ) {
    }

    public record PartitionPlanRequest(
            @NotBlank String tableName,
            @NotBlank String partitionColumn,
            @NotBlank String partitionPrefix,
            @Min(2020) int startYear,
            @Min(1) int yearsAhead,
            String notes
    ) {
    }

    public record PartitionPlanView(
            Long id,
            String tableName,
            String partitionColumn,
            String partitionPrefix,
            Integer startYear,
            Integer yearsAhead,
            String status,
            Integer lastMaterializedYear,
            String notes
    ) {
    }

    public record PartitionPreview(
            String tableName,
            Integer startYear,
            Integer endYear,
            List<String> ddlStatements
    ) {
    }
}
