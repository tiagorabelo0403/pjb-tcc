package com.tcc.pjb.backend.configs.datasource;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.datasource.routing", name = "enabled", havingValue = "true")
public class PjbAdaptiveDataPlaneService {

    private final PjbDataSourceRoutingProperties properties;
    private final PjbPrimaryReadPreferenceContext primaryReadPreferenceContext;
    private final ObjectProvider<PjbReplicaObservationService> replicaObservationServiceProvider;
    private final ObjectProvider<DataSource> writeDataSourceProvider;
    private final ObjectProvider<DataSource> readDataSourceProvider;
    private final ObjectProvider<DataSource> applicationDataSourceProvider;
    private final ObjectProvider<PjbSovereignFallbackResolver> sovereignFallbackResolverProvider;
    private final ObjectProvider<JudicialScaleProfileResolver> judicialScaleProfileResolverProvider;
    private final ObjectProvider<ReadAfterWriteConsistencyPolicy> readAfterWriteConsistencyPolicyProvider;

    public PjbAdaptiveDataPlaneService(PjbDataSourceRoutingProperties properties,
                                       PjbPrimaryReadPreferenceContext primaryReadPreferenceContext,
                                       ObjectProvider<PjbReplicaObservationService> replicaObservationServiceProvider,
                                       @Qualifier("pjbWriteRoutingDataSource") ObjectProvider<DataSource> writeDataSourceProvider,
                                       @Qualifier("pjbReadReplicaDataSource") ObjectProvider<DataSource> readDataSourceProvider,
                                       @Qualifier("dataSource") ObjectProvider<DataSource> applicationDataSourceProvider,
                                       ObjectProvider<PjbSovereignFallbackResolver> sovereignFallbackResolverProvider,
                                       ObjectProvider<JudicialScaleProfileResolver> judicialScaleProfileResolverProvider,
                                       ObjectProvider<ReadAfterWriteConsistencyPolicy> readAfterWriteConsistencyPolicyProvider) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.primaryReadPreferenceContext = Objects.requireNonNull(primaryReadPreferenceContext, "primaryReadPreferenceContext");
        this.replicaObservationServiceProvider = Objects.requireNonNull(replicaObservationServiceProvider, "replicaObservationServiceProvider");
        this.writeDataSourceProvider = Objects.requireNonNull(writeDataSourceProvider, "writeDataSourceProvider");
        this.readDataSourceProvider = Objects.requireNonNull(readDataSourceProvider, "readDataSourceProvider");
        this.applicationDataSourceProvider = Objects.requireNonNull(applicationDataSourceProvider, "applicationDataSourceProvider");
        this.sovereignFallbackResolverProvider = Objects.requireNonNull(sovereignFallbackResolverProvider, "sovereignFallbackResolverProvider");
        this.judicialScaleProfileResolverProvider = Objects.requireNonNull(judicialScaleProfileResolverProvider, "judicialScaleProfileResolverProvider");
        this.readAfterWriteConsistencyPolicyProvider = Objects.requireNonNull(readAfterWriteConsistencyPolicyProvider, "readAfterWriteConsistencyPolicyProvider");
    }


    public AdaptiveDecision decide(HttpServletRequest request) {
        String method = normalizeMethod(request == null ? null : request.getMethod());
        String path = normalizePath(request == null ? null : request.getRequestURI());
        PjbDataSourceRoutingProperties.AdaptivePlane adaptivePlane = properties.getAdaptivePlane();
        PressureSnapshot pressure = samplePressure();
        PjbReplicaObservationService.ReplicaObservationSnapshot observation = currentObservation();
        Double lagSeconds = extractDefaultLagSeconds(observation);
        boolean mutating = isMutatingMethod(method);
        boolean preferPrimaryWindow = primaryReadPreferenceContext.isPrimaryPreferred();
        boolean critical = matches(path, adaptivePlane.getPrimaryCriticalPrefixes());
        boolean cacheHot = matches(path, adaptivePlane.getHotCachePrefixes());
        boolean searchBacked = matches(path, adaptivePlane.getSearchBackedPrefixes());
        boolean asyncWrite = matches(path, adaptivePlane.getAsyncWritePrefixes());
        JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy = scalePolicy(request);

        if (!adaptivePlane.isEnabled()) {
            return decision(
                    AdaptiveMode.REPLICA_SHARED,
                    "adaptive-plane-disabled",
                    false,
                    false,
                    false,
                    false,
                    lagSeconds,
                    pressure,
                    null,
                    null,
                    false,
                    scalePolicy
            );
        }

        ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy = readAfterWriteConsistencyPolicyProvider.getIfAvailable();
        if (readAfterWriteConsistencyPolicy != null && readAfterWriteConsistencyPolicy.shouldForcePrimary()) {
            return decision(
                    AdaptiveMode.PRIMARY_STRICT,
                    "read-after-write-consistency-window",
                    true,
                    false,
                    false,
                    false,
                    lagSeconds,
                    pressure,
                    null,
                    null,
                    false,
                    scalePolicy
            );
        }

        if (preferPrimaryWindow) {
            return decision(
                    AdaptiveMode.PRIMARY_STRICT,
                    "read-your-writes-window",
                    true,
                    false,
                    false,
                    false,
                    lagSeconds,
                    pressure,
                    null,
                    null,
                    false,
                    scalePolicy
            );
        }

        if (mutating) {
            return decision(
                    asyncWrite ? AdaptiveMode.QUEUE_DEFERRED : AdaptiveMode.PRIMARY_STRICT,
                    asyncWrite ? "mutating-route-prefers-outbox-queue" : "mutating-request",
                    true,
                    false,
                    false,
                    asyncWrite,
                    lagSeconds,
                    pressure,
                    null,
                    null,
                    false,
                    scalePolicy
            );
        }

        Duration degradedTolerance = scaleDuration(
                adaptivePlane.getDegradedReplicaLagTolerance(),
                scalePolicy == null ? 1d : scalePolicy.degradedReplicaLagFactor()
        );
        if (searchBacked && (readPressureTooHigh(pressure, adaptivePlane, scalePolicy) || replicaLagTooHigh(lagSeconds, degradedTolerance))) {
            return decision(
                    AdaptiveMode.SEARCH_INDEX,
                    "search-offload-preferred",
                    false,
                    false,
                    true,
                    false,
                    lagSeconds,
                    pressure,
                    null,
                    null,
                    false,
                    scalePolicy
            );
        }

        if (cacheHot && (readPressureTooHigh(pressure, adaptivePlane, scalePolicy) || replicaLagTooHigh(lagSeconds, degradedTolerance))) {
            return decision(
                    AdaptiveMode.CACHE_HOT,
                    "cache-hot-lane-preferred",
                    false,
                    true,
                    false,
                    false,
                    lagSeconds,
                    pressure,
                    null,
                    null,
                    false,
                    scalePolicy
            );
        }

        PjbSovereignFallbackResolver.SovereignResolution sovereignResolution = sovereignResolution(request, observation, critical);
        if (sovereignResolution != null && sovereignResolution.forcePrimary()) {
            return decision(
                    AdaptiveMode.PRIMARY_STRICT,
                    sovereignResolution.reason(),
                    true,
                    false,
                    false,
                    false,
                    lagSeconds,
                    pressure,
                    null,
                    sovereignResolution.sovereignScope(),
                    sovereignResolution.fallbackActivated(),
                    scalePolicy
            );
        }
        if (sovereignResolution != null && sovereignResolution.preferredReplicaKey() != null) {
            AdaptiveMode mode = "READ".equals(sovereignResolution.preferredReplicaKey())
                    ? AdaptiveMode.REPLICA_SHARED
                    : AdaptiveMode.REPLICA_REGIONAL;
            return decision(
                    mode,
                    sovereignResolution.reason(),
                    false,
                    false,
                    false,
                    false,
                    lagSeconds,
                    pressure,
                    sovereignResolution.preferredReplicaKey(),
                    sovereignResolution.sovereignScope(),
                    sovereignResolution.fallbackActivated(),
                    scalePolicy
            );
        }

        Duration replicaLagTolerance = scaleDuration(
                adaptivePlane.getReplicaLagTolerance(),
                scalePolicy == null ? 1d : scalePolicy.replicaLagFactor()
        );
        if (critical && replicaLagTooHigh(lagSeconds, replicaLagTolerance)) {
            return decision(
                    AdaptiveMode.PRIMARY_STRICT,
                    "critical-read-route-with-replica-lag",
                    true,
                    false,
                    false,
                    false,
                    lagSeconds,
                    pressure,
                    null,
                    null,
                    false,
                    scalePolicy
            );
        }

        if (observation != null && observation.availableRegionalReplicas() > 0L) {
            return decision(
                    AdaptiveMode.REPLICA_REGIONAL,
                    "regional-replica-available",
                    false,
                    false,
                    false,
                    false,
                    lagSeconds,
                    pressure,
                    null,
                    null,
                    false,
                    scalePolicy
            );
        }

        if (observation == null || observation.failoverTrackerReplicaAvailable()) {
            return decision(
                    AdaptiveMode.REPLICA_SHARED,
                    "shared-replica-available",
                    false,
                    false,
                    false,
                    false,
                    lagSeconds,
                    pressure,
                    "READ",
                    null,
                    false,
                    scalePolicy
            );
        }

        return decision(
                AdaptiveMode.PRIMARY_STRICT,
                "replica-unavailable",
                true,
                false,
                false,
                false,
                lagSeconds,
                pressure,
                null,
                null,
                false,
                scalePolicy
        );
    }

    public AdaptiveBlueprint blueprint() {
        PjbDataSourceRoutingProperties.AdaptivePlane adaptivePlane = properties.getAdaptivePlane();
        return new AdaptiveBlueprint(
                adaptivePlane.isEnabled(),
                adaptivePlane.getReplicaLagTolerance(),
                adaptivePlane.getDegradedReplicaLagTolerance(),
                adaptivePlane.getReadPoolActiveRatioThreshold(),
                adaptivePlane.getWritePoolActiveRatioThreshold(),
                adaptivePlane.getReadPoolAwaitingThreshold(),
                adaptivePlane.getWritePoolAwaitingThreshold(),
                List.copyOf(adaptivePlane.getPrimaryCriticalPrefixes()),
                List.copyOf(adaptivePlane.getHotCachePrefixes()),
                List.copyOf(adaptivePlane.getSearchBackedPrefixes()),
                List.copyOf(adaptivePlane.getAsyncWritePrefixes()),
                properties.getSovereignFallback().isEnabled(),
                List.copyOf(properties.getSovereignFallback().getScopeToFallbackReplica().entrySet().stream().map(entry -> entry.getKey() + "->" + entry.getValue()).toList()),
                properties.getProcessualReadModels().getKafkaTopic(),
                List.of(
                        new AccelerationLane("PRIMARY_STRICT", "processos/peticionamento/protocolos", "Leitura soberana para eventos juridicos criticos e read-your-writes."),
                        new AccelerationLane("CACHE_HOT", "painel/timeline/agenda/caixa", "Leitura quente com Redis+Caffeine e invalidação por evento."),
                        new AccelerationLane("SEARCH_INDEX", "busca e indexacao", "Offload de consulta textual e filtros densos para Elastic/OpenSearch."),
                        new AccelerationLane("QUEUE_DEFERRED", "uploads, anexos e reprocessamentos", "Desacoplamento por outbox e Kafka para aliviar o banco transacional."),
                        new AccelerationLane("REPLICA_REGIONAL", "tribunais e UFs", "Leitura regionalizada para reduzir latencia e espalhar carga no plano federado.")
                )
        );
    }

    private PressureSnapshot samplePressure() {
        DataSource writeSource = choose(writeDataSourceProvider.getIfAvailable(), applicationDataSourceProvider.getIfAvailable());
        DataSource readSource = choose(readDataSourceProvider.getIfAvailable(), writeSource);
        DataSourceIntrospectionSupport.PoolSnapshot writeSnapshot = DataSourceIntrospectionSupport.snapshot(writeSource);
        DataSourceIntrospectionSupport.PoolSnapshot readSnapshot = DataSourceIntrospectionSupport.snapshot(readSource);
        return new PressureSnapshot(writeSnapshot.activeRatio(), readSnapshot.activeRatio(), writeSnapshot.awaiting(), readSnapshot.awaiting());
    }

    private PjbReplicaObservationService.ReplicaObservationSnapshot currentObservation() {
        PjbReplicaObservationService service = replicaObservationServiceProvider.getIfAvailable();
        if (service == null) {
            return null;
        }
        return service.currentSnapshot();
    }

    private boolean readPressureTooHigh(PressureSnapshot pressure, PjbDataSourceRoutingProperties.AdaptivePlane adaptivePlane, JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy) {
        double factor = scalePolicy == null ? 1d : scalePolicy.readPressureFactor();
        double activeRatioThreshold = clamp(adaptivePlane.getReadPoolActiveRatioThreshold() * factor, 0.35d, 0.99d);
        int awaitingThreshold = Math.max(1, (int) Math.round(adaptivePlane.getReadPoolAwaitingThreshold() * factor));
        return pressure.readPoolActiveRatio() >= activeRatioThreshold
                || pressure.readPoolAwaiting() >= awaitingThreshold;
    }

    private boolean replicaLagTooHigh(Double lagSeconds, Duration tolerance) {
        if (lagSeconds == null || tolerance == null || tolerance.isNegative() || tolerance.isZero()) {
            return false;
        }
        return lagSeconds.doubleValue() >= tolerance.toMillis() / 1000d;
    }

    private Double extractDefaultLagSeconds(PjbReplicaObservationService.ReplicaObservationSnapshot observation) {
        if (observation == null || observation.read() == null) {
            return null;
        }
        return observation.read().replayLagSeconds();
    }

    private boolean matches(String path, List<String> prefixes) {
        if (path == null || path.isBlank() || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isBlank() && path.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isMutatingMethod(String method) {
        return switch (method) {
            case "GET", "HEAD", "OPTIONS", "TRACE" -> false;
            default -> true;
        };
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            return "GET";
        }
        return method.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.trim();
    }

    private DataSource choose(DataSource preferred, DataSource fallback) {
        return preferred != null ? preferred : fallback;
    }

    private AdaptiveDecision decision(AdaptiveMode mode,
                                      String reason,
                                      boolean forcePrimary,
                                      boolean cacheRecommended,
                                      boolean searchRecommended,
                                      boolean asyncRecommended,
                                      Double replicaLagSeconds,
                                      PressureSnapshot pressure,
                                      String preferredReplicaKey,
                                      String sovereignScope,
                                      boolean sovereignFallbackActivated,
                                      JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy) {
        return new AdaptiveDecision(
                mode,
                reason,
                forcePrimary,
                cacheRecommended,
                searchRecommended,
                asyncRecommended,
                replicaLagSeconds,
                pressure.readPoolActiveRatio(),
                pressure.writePoolActiveRatio(),
                pressure.readPoolAwaiting(),
                pressure.writePoolAwaiting(),
                preferredReplicaKey,
                sovereignScope,
                sovereignFallbackActivated,
                scalePolicy == null ? null : scalePolicy.profile().name(),
                scalePolicy == null ? null : scalePolicy.instanceClass(),
                scalePolicy == null ? null : scalePolicy.branchClass()
        );
    }

    private PjbSovereignFallbackResolver.SovereignResolution sovereignResolution(HttpServletRequest request,
                                                                                 PjbReplicaObservationService.ReplicaObservationSnapshot observation,
                                                                                 boolean critical) {
        PjbSovereignFallbackResolver resolver = sovereignFallbackResolverProvider.getIfAvailable();
        if (resolver == null) {
            return null;
        }
        return resolver.resolve(request, observation, critical);
    }

    private JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy(HttpServletRequest request) {
        JudicialScaleProfileResolver resolver = judicialScaleProfileResolverProvider.getIfAvailable();
        return resolver == null ? null : resolver.resolvePolicy(request);
    }

    private Duration scaleDuration(Duration duration, double factor) {
        JudicialScaleProfileResolver resolver = judicialScaleProfileResolverProvider.getIfAvailable();
        if (resolver == null) {
            if (duration == null) {
                return null;
            }
            long millis = Math.max(1L, Math.round(duration.toMillis() * clamp(factor, 0.25d, 4d)));
            return Duration.ofMillis(millis);
        }
        return resolver.scaleDuration(duration, factor);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum AdaptiveMode {
        PRIMARY_STRICT,
        REPLICA_REGIONAL,
        REPLICA_SHARED,
        CACHE_HOT,
        SEARCH_INDEX,
        QUEUE_DEFERRED
    }

    public record AdaptiveDecision(AdaptiveMode mode,
                                   String reason,
                                   boolean forcePrimary,
                                   boolean cacheRecommended,
                                   boolean searchRecommended,
                                   boolean asyncRecommended,
                                   Double replicaLagSeconds,
                                   double readPressureRatio,
                                   double writePressureRatio,
                                   int readThreadsAwaiting,
                                   int writeThreadsAwaiting,
                                   String preferredReplicaKey,
                                   String sovereignScope,
                                   boolean sovereignFallbackActivated,
                                   String scaleProfile,
                                   String scaleInstanceClass,
                                   String scaleBranchClass) {
    }

    public record AdaptiveBlueprint(boolean enabled,
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
                                    List<AccelerationLane> accelerationLanes) {
    }

    public record AccelerationLane(String mode, String target, String rationale) {
    }

    private record PressureSnapshot(double writePoolActiveRatio,
                                    double readPoolActiveRatio,
                                    int writePoolAwaiting,
                                    int readPoolAwaiting) {
    }
}
