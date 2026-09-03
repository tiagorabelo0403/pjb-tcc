package com.tcc.pjb.backend.service.infra;

import com.tcc.pjb.backend.configs.datasource.PjbAdaptiveDataPlaneService;
import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.configs.security.governance.ApiRouteGovernanceProperties;
import com.tcc.pjb.backend.core.jobs.runtime.JobDispatcherProperties;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfile;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleRuntimePolicyService;
import com.tcc.pjb.backend.model.repository.ProcessualReadModelMaterializationTrailRepository;
import com.tcc.pjb.backend.model.repository.ProcessualReadModelProjectionRepository;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class ScaleArchitectureService {

    private final ProcessualReadModelProjectionRepository processualReadModelProjectionRepository;
    private final ProcessualReadModelMaterializationTrailRepository processualReadModelMaterializationTrailRepository;
    private final ObjectProvider<PjbAdaptiveDataPlaneService> adaptiveDataPlaneServiceProvider;
    private final ObjectProvider<PjbDataSourceRoutingProperties> dataSourceRoutingPropertiesProvider;
    private final ObjectProvider<PjbProcessualReadModelMeshService> processualReadModelMeshServiceProvider;
    private final ObjectProvider<PjbProcessualReadModelRecompositionQueueService> recompositionQueueServiceProvider;
    private final ObjectProvider<ApiRouteGovernanceProperties> apiRouteGovernancePropertiesProvider;
    private final ObjectProvider<JobDispatcherProperties> jobDispatcherPropertiesProvider;
    private final JudicialScaleProfileResolver judicialScaleProfileResolver;
    private final JudicialScaleRuntimePolicyService judicialScaleRuntimePolicyService;
    private final ScaleCachePartitionGovernanceService cachePartitionGovernanceService;
    private final ScaleSecretariatModelGovernanceService secretariatModelGovernanceService;
    private final ScaleProceduralGovernanceService proceduralGovernanceService;
    private final Environment environment;

    public ScaleArchitectureService(ProcessualReadModelProjectionRepository processualReadModelProjectionRepository,
                                    ProcessualReadModelMaterializationTrailRepository processualReadModelMaterializationTrailRepository,
                                    ObjectProvider<PjbAdaptiveDataPlaneService> adaptiveDataPlaneServiceProvider,
                                    ObjectProvider<PjbDataSourceRoutingProperties> dataSourceRoutingPropertiesProvider,
                                    ObjectProvider<PjbProcessualReadModelMeshService> processualReadModelMeshServiceProvider,
                                    ObjectProvider<PjbProcessualReadModelRecompositionQueueService> recompositionQueueServiceProvider,
                                    ObjectProvider<ApiRouteGovernanceProperties> apiRouteGovernancePropertiesProvider,
                                    ObjectProvider<JobDispatcherProperties> jobDispatcherPropertiesProvider,
                                    JudicialScaleProfileResolver judicialScaleProfileResolver,
                                    JudicialScaleRuntimePolicyService judicialScaleRuntimePolicyService,
                                    ScaleCachePartitionGovernanceService cachePartitionGovernanceService,
                                    ScaleSecretariatModelGovernanceService secretariatModelGovernanceService,
                                    ScaleProceduralGovernanceService proceduralGovernanceService,
                                    Environment environment) {
        this.processualReadModelProjectionRepository = Objects.requireNonNull(processualReadModelProjectionRepository);
        this.processualReadModelMaterializationTrailRepository = Objects.requireNonNull(processualReadModelMaterializationTrailRepository);
        this.adaptiveDataPlaneServiceProvider = Objects.requireNonNull(adaptiveDataPlaneServiceProvider);
        this.dataSourceRoutingPropertiesProvider = Objects.requireNonNull(dataSourceRoutingPropertiesProvider);
        this.processualReadModelMeshServiceProvider = Objects.requireNonNull(processualReadModelMeshServiceProvider);
        this.recompositionQueueServiceProvider = Objects.requireNonNull(recompositionQueueServiceProvider);
        this.apiRouteGovernancePropertiesProvider = Objects.requireNonNull(apiRouteGovernancePropertiesProvider);
        this.jobDispatcherPropertiesProvider = Objects.requireNonNull(jobDispatcherPropertiesProvider);
        this.judicialScaleProfileResolver = Objects.requireNonNull(judicialScaleProfileResolver);
        this.judicialScaleRuntimePolicyService = Objects.requireNonNull(judicialScaleRuntimePolicyService);
        this.cachePartitionGovernanceService = Objects.requireNonNull(cachePartitionGovernanceService);
        this.secretariatModelGovernanceService = Objects.requireNonNull(secretariatModelGovernanceService);
        this.proceduralGovernanceService = Objects.requireNonNull(proceduralGovernanceService);
        this.environment = Objects.requireNonNull(environment);
    }

    public List<ScaleCachePartitionGovernanceService.CachePolicyView> listarPoliticasCache() {
        return cachePartitionGovernanceService.listarPoliticasCache();
    }

    public ScaleCachePartitionGovernanceService.CachePolicyView salvarPoliticaCache(ScaleCachePartitionGovernanceService.CachePolicyRequest request) {
        return cachePartitionGovernanceService.salvarPoliticaCache(request);
    }

    @PjbTransactionalBudget(operation = "infra.scale-architecture.listar-planos-particao", maxMillis = 3000)
    public List<ScaleCachePartitionGovernanceService.PartitionPlanView> listarPlanosParticao() {
        return cachePartitionGovernanceService.listarPlanosParticao();
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

    public ScaleSecretariatModelGovernanceService.JudicialSecretariatModelGovernanceView judicialSecretariatModelsView() {
        return secretariatModelGovernanceService.judicialSecretariatModelsView();
    }

    public ScaleSecretariatModelGovernanceService.JudicialOperationalDeskGovernanceView judicialOperationalDesksView() {
        return secretariatModelGovernanceService.judicialOperationalDesksView();
    }

    public ScaleSecretariatModelGovernanceService.JudicialOperationalActionGovernanceView judicialOperationalActionsView() {
        return secretariatModelGovernanceService.judicialOperationalActionsView();
    }

    public ScaleSecretariatModelGovernanceService.JudicialOperationalTransactionGovernanceView judicialOperationalTransactionsView() {
        return secretariatModelGovernanceService.judicialOperationalTransactionsView();
    }

    public Object judicialProceduralCoverageView() {
        return proceduralGovernanceService.judicialProceduralCoverageView();
    }

    public Object judicialProceduralCoverageDetailView(String rito) {
        return proceduralGovernanceService.judicialProceduralCoverageDetailView(rito);
    }

    public Object judicialProceduralPlaybookView() {
        return proceduralGovernanceService.judicialProceduralPlaybookView();
    }

    public Object judicialProceduralPlaybookDetailView(String rito) {
        return proceduralGovernanceService.judicialProceduralPlaybookDetailView(rito);
    }

    public Object judicialTribunalVariationView() {
        return proceduralGovernanceService.judicialTribunalVariationView();
    }

    public Object judicialTribunalVariationDetailView(String tribunalCodigo, String rito, String unidadeCodigo, String tipoJustica) {
        return proceduralGovernanceService.judicialTribunalVariationDetailView(tribunalCodigo, rito, unidadeCodigo, tipoJustica);
    }

    public ScaleSecretariatModelGovernanceService.JudicialInstitutionalAlignmentGovernanceView judicialInstitutionalAlignmentView() {
        return secretariatModelGovernanceService.judicialInstitutionalAlignmentView();
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

    public ScaleCachePartitionGovernanceService.PartitionPlanView salvarPlanoParticao(ScaleCachePartitionGovernanceService.PartitionPlanRequest request) {
        return cachePartitionGovernanceService.salvarPlanoParticao(request);
    }

    public ScaleCachePartitionGovernanceService.PartitionPreview previewMaterializacao(String tableName) {
        return cachePartitionGovernanceService.previewMaterializacao(tableName);
    }

    public ScaleCachePartitionGovernanceService.PartitionPreview materializar(String tableName) {
        return cachePartitionGovernanceService.materializar(tableName);
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

}
