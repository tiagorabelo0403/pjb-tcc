package com.tcc.pjb.backend.configs.security.hardening;

import com.tcc.pjb.backend.platform.runtime.PjbKafkaPressureService;
import com.tcc.pjb.backend.platform.runtime.PjbLivePressureService;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimeDrainService;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimePressureService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class PjbOperationalAdmissionService {

    private final PjbOperationalAdmissionProperties properties;
    private final PjbRuntimePressureService pressureService;
    private final PjbRuntimeDrainService drainService;
    private final PjbLivePressureService livePressureService;
    private final PjbKafkaPressureService kafkaPressureService;
    private final ConcurrentMap<String, CachedDecision> decisionCache = new ConcurrentHashMap<>();

    public PjbOperationalAdmissionService(PjbOperationalAdmissionProperties properties,
                                          PjbRuntimePressureService pressureService,
                                          PjbRuntimeDrainService drainService,
                                          PjbLivePressureService livePressureService,
                                          PjbKafkaPressureService kafkaPressureService) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.pressureService = Objects.requireNonNull(pressureService, "pressureService");
        this.drainService = Objects.requireNonNull(drainService, "drainService");
        this.livePressureService = Objects.requireNonNull(livePressureService, "livePressureService");
        this.kafkaPressureService = Objects.requireNonNull(kafkaPressureService, "kafkaPressureService");
    }

    public Decision evaluate(String method, String uri) {
        return evaluate(method, uri, OperationShape.empty());
    }

    public Decision evaluate(String method,
                             String uri,
                             OperationShape shape) {
        if (!properties.isEnabled() || isExempt(uri) || !isGuarded(uri)) {
            return Decision.allow("exempt", "EXEMPT", RoutePriority.EXEMPT);
        }
        OperationShape safeShape = shape == null ? OperationShape.empty() : shape;
        Instant now = Instant.now();
        String cacheKey = cacheKey(method, uri, safeShape);
        CachedDecision cached = decisionCache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.decision();
        }
        Decision decision = computeDecision(method, uri, safeShape);
        decisionCache.put(cacheKey, new CachedDecision(decision, now.plus(properties.getDecisionTtl())));
        trimCache(now);
        return decision;
    }

    private Decision computeDecision(String method, String uri, OperationShape shape) {
        boolean write = isWrite(method) || startsWithAny(uri, properties.getWriteSensitivePrefixes());
        boolean expensive = startsWithAny(uri, properties.getExpensivePrefixes()) || write;
        String bucket = classifyBucket(method, uri, write, expensive, shape);
        boolean stream = bucket.startsWith("stream");
        boolean bulk = bucket.startsWith("bulk");
        boolean export = bucket.startsWith("export");
        boolean veryLarge = shape.contentLengthBytes() >= properties.getVeryLargeContentLengthBytes()
                || shape.requestedPageSize() >= properties.getLargePageSizeThreshold() * 2
                || shape.estimatedItems() >= properties.getLargeEstimatedItemsThreshold() * 2;
        boolean large = veryLarge
                || shape.contentLengthBytes() >= properties.getLargeContentLengthBytes()
                || shape.requestedPageSize() >= properties.getLargePageSizeThreshold()
                || shape.estimatedItems() >= properties.getLargeEstimatedItemsThreshold();
        PjbRuntimeDrainService.Snapshot drain = drainService.snapshot();
        PjbRuntimePressureService.Snapshot snapshot = pressureService.snapshot();
        PjbLivePressureService.Snapshot live = livePressureService.snapshot(snapshot.warmingUp());
        PjbKafkaPressureService.Snapshot kafka = kafkaPressureService.snapshot(snapshot.warmingUp());
        RoutePriority priority = determinePriority(uri, bucket, snapshot.componentRole(), write, expensive, large);
        int softThreshold = effectiveThreshold(properties.getSoftPressureScoreThreshold(), priority, false);
        int hardThreshold = effectiveThreshold(properties.getHardPressureScoreThreshold(), priority, true);
        boolean roleApi = "api".equals(snapshot.componentRole());
        boolean roleWorker = "worker".equals(snapshot.componentRole());
        List<String> reservedLanes = reservedLanes(snapshot.componentRole());
        List<String> preferredLanes = preferredLanes(snapshot.componentRole(), bucket, uri, write, stream, bulk, export, large);
        long reservedLaneHotspots = snapshot.overloadedLaneCount(reservedLanes);
        long preferredLaneHotspots = snapshot.overloadedLaneCount(preferredLanes);
        double reservedLaneAverageUtilization = snapshot.averageLaneUtilization(reservedLanes);
        int reservedLaneHeadroomScore = snapshot.laneHeadroomScore(reservedLanes);
        double preferredLaneAverageUtilization = snapshot.averageLaneUtilization(preferredLanes);
        int preferredLaneHeadroomScore = snapshot.laneHeadroomScore(preferredLanes);
        boolean reservedLaneBudgetBreach = snapshot.laneBudgetBreached(
                reservedLanes,
                properties.getReservedLaneAverageUtilizationThreshold(),
                properties.getReservedLaneHeadroomScoreThreshold());
        boolean preferredLaneBudgetBreach = snapshot.laneBudgetBreached(
                preferredLanes,
                properties.getPreferredLaneAverageUtilizationThreshold(),
                properties.getPreferredLaneHeadroomScoreThreshold());
        boolean reservedLanePressure = reservedLaneHotspots > properties.getMaxReservedLaneHotspotsBeforeReject();
        boolean reservedLaneCollapse = preferredLaneHotspots > properties.getMaxReservedLaneHotspotsBeforeCollapse()
                || preferredLanes.stream().anyMatch(snapshot::hasCriticalOverloadedLane);
        if (properties.isRejectDuringDrain() && drain.draining()) {
            return Decision.reject(true, properties.getHardRejectionStatus(), "RUNTIME_DRAINING", "A instância está drenando tráfego e não aceita novas requisições operacionais.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectDuringWarmup() && snapshot.warmingUp() && (expensive || stream || bulk || export || large)) {
            return Decision.reject(false, properties.getSoftRejectionStatus(), "RUNTIME_WARMING_UP", "A instância ainda está em aquecimento operacional e preserva rotas mais pesadas temporariamente.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectEventStreamOnLiveSurge() && live.criticalSurge() && (stream || shape.eventStreamRequested())) {
            return Decision.reject(false, properties.getSoftRejectionStatus(), "LIVE_STREAM_SURGE", "A malha SSE entrou em surto de assinaturas e a instância preservou fluxos contínuos temporariamente.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectWriteOnKafkaBackpressure() && kafka.degraded() && write && (roleApi || roleWorker)) {
            boolean hard = kafka.critical() || veryLarge || priority == RoutePriority.LOW;
            return Decision.reject(hard, hard ? properties.getHardRejectionStatus() : properties.getSoftRejectionStatus(), "KAFKA_BACKPRESSURE", hard ? "A trilha de mensageria entrou em contenção e a instância bloqueou mutações para preservar estabilidade." : "A trilha Kafka está sob pressão e a instância adiou mutações temporariamente.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectWriteOnCriticalDatasourceRunaway() && snapshot.criticalDatasourceRunaway() && write) {
            return Decision.reject(true, properties.getHardRejectionStatus(), "CRITICAL_DATASOURCE_RUNAWAY", "O pool crítico do banco entrou em pressão acelerada e a instância bloqueou mutações para preservar estabilidade.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectExpensiveOnCriticalMemoryRunaway() && snapshot.criticalMemoryRunaway() && (expensive || stream || export || bulk || large)) {
            boolean hard = write || bulk || roleWorker || veryLarge || priority == RoutePriority.LOW;
            return Decision.reject(hard, hard ? properties.getHardRejectionStatus() : properties.getSoftRejectionStatus(), "CRITICAL_MEMORY_RUNAWAY", hard ? "A instância entrou em contenção de memória crítica e bloqueou a trilha operacional para evitar degradação em cascata." : "A instância detectou runaway de memória e ativou brownout preventivo para rotas mais custosas.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectExpensiveOnCriticalGcPressure() && snapshot.criticalGcPressure() && (expensive || stream || export || bulk || large)) {
            boolean hard = write || bulk || veryLarge || priority == RoutePriority.LOW;
            return Decision.reject(hard, hard ? properties.getHardRejectionStatus() : properties.getSoftRejectionStatus(), "CRITICAL_GC_PRESSURE", hard ? "A instância entrou em contenção severa de GC e bloqueou a trilha operacional para evitar degradação em cascata." : "A instância detectou pressão crítica de GC e ativou brownout preventivo para rotas mais custosas.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectLowPriorityEarlier() && priority == RoutePriority.LOW && (expensive || large || export || bulk || stream)
                && (snapshot.pressureScore() >= Math.max(25, softThreshold - 8) || snapshot.trend().risingFast() || snapshot.schedulerTrendingUp() || snapshot.criticalGcPressure() || (snapshot.gc() != null && snapshot.gc().risingFast()) || live.degraded() || kafka.degraded())) {
            boolean hard = write || veryLarge || snapshot.pressureScore() >= hardThreshold || snapshot.trend().sustained();
            return Decision.reject(hard, hard ? properties.getHardRejectionStatus() : properties.getSoftRejectionStatus(), "LOW_PRIORITY_BROWNOUT", hard ? "A instância preservou trilhas prioritárias e bloqueou temporariamente uma rota de baixa prioridade em estado crítico." : "A instância preservou trilhas prioritárias e adiou temporariamente uma rota de baixa prioridade sob pressão.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }

        if (properties.isRejectLowPriorityOnReservedLaneBudgetBreach()
                && priority == RoutePriority.LOW
                && reservedLaneBudgetBreach
                && (expensive || large || export || bulk || stream || write)) {
            boolean hard = reservedLaneAverageUtilization >= Math.max(0.90d, properties.getReservedLaneAverageUtilizationThreshold() + 0.08d)
                    || reservedLaneHeadroomScore <= Math.max(4, properties.getReservedLaneHeadroomScoreThreshold() / 2)
                    || veryLarge;
            String detail = hard
                    ? "A instância protegeu a capacidade reservada das lanes críticas e bloqueou a rota de baixa prioridade antes de colapso operacional."
                    : "A instância protegeu a capacidade reservada das lanes críticas e adiou a rota de baixa prioridade sob orçamento apertado.";
            return Decision.reject(hard, hard ? properties.getHardRejectionStatus() : properties.getSoftRejectionStatus(), "RESERVED_LANE_BUDGET_BREACH", detail, priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }

        if (properties.isRejectNormalPriorityOnPreferredLaneBudgetBreach()
                && priority == RoutePriority.NORMAL
                && preferredLaneBudgetBreach
                && (expensive || large || export || bulk || stream || write)
                && !priority.promotesAvailability()) {
            boolean hard = preferredLaneAverageUtilization >= Math.max(0.94d, properties.getPreferredLaneAverageUtilizationThreshold() + 0.06d)
                    || preferredLaneHeadroomScore <= Math.max(3, properties.getPreferredLaneHeadroomScoreThreshold() / 2)
                    || reservedLaneCollapse;
            String detail = hard
                    ? "A trilha preferencial do endpoint entrou em orçamento crítico e a instância bloqueou temporariamente a operação para preservar estabilidade." 
                    : "A trilha preferencial do endpoint entrou em orçamento apertado e a instância adiou temporariamente a operação.";
            return Decision.reject(hard, hard ? properties.getHardRejectionStatus() : properties.getSoftRejectionStatus(), "PREFERRED_LANE_BUDGET_BREACH", detail, priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectLowPriorityOnReservedLaneOverload()
                && priority == RoutePriority.LOW
                && reservedLanePressure
                && (expensive || large || export || bulk || stream || write)) {
            boolean hard = reservedLaneCollapse || write || veryLarge || snapshot.pressureScore() >= hardThreshold;
            return Decision.reject(hard,
                    hard ? properties.getHardRejectionStatus() : properties.getSoftRejectionStatus(),
                    hard ? "RESERVED_LANE_COLLAPSE" : "RESERVED_LANE_OVERLOAD",
                    hard ? "A instância preservou lanes reservadas do papel operacional atual e bloqueou uma rota de baixa prioridade para evitar cascata." : "A instância preservou lanes reservadas do papel operacional atual e adiou uma rota de baixa prioridade sob pressão local.",
                    priority,
                    snapshot,
                    drain,
                    live,
                    kafka,
                    write,
                    expensive,
                    bucket,
                    method,
                    uri,
                    shape.shapeKey());
        }
        if (properties.isRejectNormalPriorityOnReservedLaneCollapse()
                && priority == RoutePriority.NORMAL
                && reservedLaneCollapse
                && (expensive || large || stream || bulk || export)
                && (snapshot.pressureScore() >= Math.max(30, softThreshold - 5) || snapshot.trend().risingFast() || kafka.degraded() || live.degraded())) {
            return Decision.reject(false,
                    properties.getSoftRejectionStatus(),
                    "NORMAL_PRIORITY_RESERVED_LANE_COLLAPSE",
                    "A instância detectou colapso nas lanes reservadas da trilha operacional e ativou contenção seletiva para rotas normais custosas.",
                    priority,
                    snapshot,
                    drain,
                    live,
                    kafka,
                    write,
                    expensive,
                    bucket,
                    method,
                    uri,
                    shape.shapeKey());
        }
        if (properties.isPreserveHighPriorityReadOnSoftPressure()
                && !write
                && priority.promotesAvailability()
                && !veryLarge
                && !snapshot.criticalDatasourceRunaway()
                && !snapshot.criticalMemoryRunaway()
                && !live.criticalSurge()
                && !kafka.critical()
                && (!reservedLaneCollapse || priority == RoutePriority.CRITICAL)
                && snapshot.pressureScore() < hardThreshold) {
            return Decision.allow("priority-preserved", "PRIORITY_PRESERVED", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectVeryLargeWriteOnPressure() && write && veryLarge && (snapshot.pressureScore() >= softThreshold || snapshot.trend().risingFast() || kafka.degraded())) {
            return Decision.reject(true, properties.getHardRejectionStatus(), "VERY_LARGE_WRITE_BROWNOUT", "A operação mutante é grande para o estado operacional atual e foi bloqueada preventivamente.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectVeryLargeReadOnPressure() && !write && large && (snapshot.schedulerTrendingUp() || live.degraded() || snapshot.trend().sustained())) {
            return Decision.reject(false, properties.getSoftRejectionStatus(), "LARGE_READ_BROWNOUT", "A leitura solicitada é grande para o estado operacional atual e foi adiada temporariamente.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (snapshot.pressureScore() >= hardThreshold && (expensive || bulk || export || large)) {
            return Decision.reject(true, properties.getHardRejectionStatus(), "PRESSURE_HARD_LIMIT", "A instância entrou em contenção operacional por pressão alta e rejeitou a trilha mais custosa temporariamente.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectStreamOnMemoryPressure() && stream && snapshot.memory() != null && snapshot.memory().degraded()) {
            return Decision.reject(false, properties.getSoftRejectionStatus(), "STREAM_MEMORY_BROWNOUT", "A instância detectou pressão de memória e preservou temporariamente fluxos contínuos para manter estabilidade geral.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectExportOnPressureRising() && export && (snapshot.trend().risingFast() || (snapshot.memory() != null && snapshot.memory().heapRisingFast()) || live.risingFast())) {
            return Decision.reject(false, properties.getSoftRejectionStatus(), "EXPORT_BROWNOUT", "A pressão operacional está subindo rapidamente e a instância adiou exportações pesadas temporariamente.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectExpensiveOnSchedulerTrendingUp() && snapshot.schedulerTrendingUp() && (roleApi ? (stream || export || expensive || large) : (bulk || write || expensive || large))) {
            return Decision.reject(false, properties.getSoftRejectionStatus(), "SCHEDULER_TRENDING_UP", "O scheduler operacional está acelerando backlog e a instância reduziu admissões pesadas temporariamente.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectExpensiveWhenPressureRising() && snapshot.trend().risingFast() && (expensive || export || large || (roleWorker && bulk))) {
            return Decision.reject(false, properties.getSoftRejectionStatus(), "PRESSURE_TRENDING_UP", "A pressão operacional está subindo rapidamente e a instância ativou brownout preventivo para trilhas mais pesadas.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectBulkOnPressureSustained() && bulk && snapshot.trend().sustained() && snapshot.pressureScore() >= softThreshold) {
            boolean hard = write || roleWorker || veryLarge || priority == RoutePriority.LOW;
            return Decision.reject(hard, hard ? properties.getHardRejectionStatus() : properties.getSoftRejectionStatus(), "BULK_SUSTAINED_BROWNOUT", hard ? "A instância preservou a malha operacional e bloqueou processamento em lote sob pressão sustentada." : "A instância entrou em brownout sustentado e adiou leitura em lote temporariamente.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        if (properties.isRejectOnPressureSustained() && snapshot.trend().sustained() && snapshot.pressureScore() >= softThreshold && (expensive || large)) {
            return Decision.reject(false, properties.getSoftRejectionStatus(), "PRESSURE_SUSTAINED", "A instância entrou em brownout por pressão sustentada e pede novo envio em instantes.", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
        }
        return Decision.allow("steady", "STEADY", priority, snapshot, drain, live, kafka, write, expensive, bucket, method, uri, shape.shapeKey());
    }

    private RoutePriority determinePriority(String uri,
                                            String bucket,
                                            String componentRole,
                                            boolean write,
                                            boolean expensive,
                                            boolean large) {
        String normalized = uri == null ? "" : uri.toLowerCase(Locale.ROOT);
        if (startsWithAny(normalized, properties.getCriticalPrefixes())) {
            return RoutePriority.CRITICAL;
        }
        if (containsAny(normalized, properties.getLowPriorityPrefixes()) || bucket.startsWith("export") || bucket.startsWith("bulk-upload") || (large && expensive && !write)) {
            return RoutePriority.LOW;
        }
        if ("worker".equals(componentRole) && startsWithAny(normalized, properties.getWorkerHighPriorityPrefixes())) {
            return RoutePriority.HIGH;
        }
        if ("api".equals(componentRole) && startsWithAny(normalized, properties.getApiHighPriorityPrefixes())) {
            return RoutePriority.HIGH;
        }
        if (!write && (bucket.equals("light") || bucket.equals("read-paged"))) {
            return RoutePriority.HIGH;
        }
        return RoutePriority.NORMAL;
    }

    private int effectiveThreshold(int base, RoutePriority priority, boolean hardThreshold) {
        return switch (priority) {
            case CRITICAL -> Math.min(100, base + (hardThreshold ? 10 : 12));
            case HIGH -> Math.min(100, base + (hardThreshold ? 4 : 6));
            case LOW -> Math.max(20, base - (hardThreshold ? 8 : 10));
            case NORMAL, EXEMPT -> base;
        };
    }

    private void trimCache(Instant now) {
        int maxEntries = Math.max(32, properties.getDecisionCacheMaxEntries());
        if (decisionCache.size() <= maxEntries) {
            return;
        }
        decisionCache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        if (decisionCache.size() > maxEntries * 2) {
            decisionCache.clear();
        }
    }

    private String cacheKey(String method, String uri, OperationShape shape) {
        return (method == null ? "" : method) + '|' + (uri == null ? "" : uri) + '|' + shape.shapeKey();
    }

    private String classifyBucket(String method, String uri, boolean write, boolean expensive, OperationShape shape) {
        String normalizedUri = uri == null ? "" : uri.toLowerCase(Locale.ROOT);
        if (shape.eventStreamRequested() || containsAny(normalizedUri, "/stream", "/sse", "/eventos", "/votos/tempo-real")) {
            return shape.estimatedItems() >= properties.getLargeEstimatedItemsThreshold() ? "stream-heavy" : "stream";
        }
        if (containsAny(normalizedUri, "/export", "/relatorio", "/report", "/download")) {
            return shape.estimatedItems() >= properties.getLargeEstimatedItemsThreshold() ? "export-heavy" : "export";
        }
        if (containsAny(normalizedUri, "/bulk", "/lote", "/massivo") || shape.estimatedItems() >= properties.getLargeEstimatedItemsThreshold()) {
            return write ? "bulk-write" : "bulk-read";
        }
        if (write && shape.contentLengthBytes() >= properties.getVeryLargeContentLengthBytes()) {
            return "bulk-upload";
        }
        if (write) {
            return expensive ? "write-expensive" : "write-light";
        }
        if (shape.requestedPageSize() >= properties.getLargePageSizeThreshold()) {
            return expensive ? "read-expensive-paged" : "read-paged";
        }
        return expensive ? "read-expensive" : "light";
    }

    private List<String> reservedLanes(String componentRole) {
        if ("worker".equals(componentRole)) {
            return properties.getWorkerReservedLanes();
        }
        if ("api".equals(componentRole)) {
            return properties.getApiReservedLanes();
        }
        return List.of("io");
    }

    private List<String> preferredLanes(String componentRole,
                                        String bucket,
                                        String uri,
                                        boolean write,
                                        boolean stream,
                                        boolean bulk,
                                        boolean export,
                                        boolean large) {
        if (stream || bucket.startsWith("stream")) {
            return List.of("live", "external-io");
        }
        if (export || bucket.startsWith("export")) {
            return "worker".equals(componentRole) ? List.of("job", "io") : List.of("io", "external-io");
        }
        if (bulk || bucket.startsWith("bulk")) {
            return "worker".equals(componentRole) || write ? List.of("job", "io") : List.of("io");
        }
        if (write) {
            return "worker".equals(componentRole) ? List.of("job", "io") : List.of("io");
        }
        if (large || bucket.contains("paged") || containsAny(uri == null ? "" : uri.toLowerCase(Locale.ROOT), "/consulta", "/painel", "/search", "/pesquisa")) {
            return "api".equals(componentRole) ? List.of("external-io", "io") : List.of("io");
        }
        return reservedLanes(componentRole);
    }

    private boolean containsAny(String value, String... fragments) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String fragment : fragments) {
            if (fragment != null && !fragment.isBlank() && value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String value, List<String> fragments) {
        if (value == null || value.isBlank() || fragments == null || fragments.isEmpty()) {
            return false;
        }
        for (String fragment : fragments) {
            if (fragment != null && !fragment.isBlank() && value.contains(fragment.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isExempt(String uri) {
        return startsWithAny(uri, properties.getExemptPrefixes());
    }

    private boolean isGuarded(String uri) {
        return startsWithAny(uri, properties.getGuardedPrefixes());
    }

    private boolean startsWithAny(String uri, List<String> prefixes) {
        if (uri == null || uri.isBlank() || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isBlank() && uri.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isWrite(String method) {
        if (method == null || method.isBlank()) {
            return false;
        }
        String normalized = method.trim().toUpperCase(Locale.ROOT);
        return HttpMethod.POST.matches(normalized)
                || HttpMethod.PUT.matches(normalized)
                || HttpMethod.PATCH.matches(normalized)
                || HttpMethod.DELETE.matches(normalized);
    }

    public record OperationShape(long contentLengthBytes,
                                 int requestedPageSize,
                                 long estimatedItems,
                                 boolean eventStreamRequested) {

        public static OperationShape empty() {
            return new OperationShape(0L, 0, 0L, false);
        }

        public String shapeKey() {
            return (contentLengthBytes / 524_288L) + ":" + (requestedPageSize / 100) + ":" + (estimatedItems / 1000L) + ":" + eventStreamRequested;
        }
    }

    public enum RoutePriority {
        EXEMPT,
        CRITICAL,
        HIGH,
        NORMAL,
        LOW;

        public boolean promotesAvailability() {
            return this == CRITICAL || this == HIGH;
        }
    }

    public record Decision(boolean allowed,
                           boolean hardRejection,
                           int status,
                           String code,
                           String detail,
                           String bucket,
                           RoutePriority priority,
                           String method,
                           String uri,
                           String shapeKey,
                           boolean writeSensitive,
                           boolean expensive,
                           PjbRuntimePressureService.Snapshot pressure,
                           PjbRuntimeDrainService.Snapshot drain,
                           PjbLivePressureService.Snapshot live,
                           PjbKafkaPressureService.Snapshot kafka) {

        private static Decision allow(String detail, String code, RoutePriority priority) {
            return new Decision(true, false, 200, code, detail, "allow", priority, null, null, "allow", false, false, null, null, null, null);
        }

        private static Decision allow(String detail,
                                      String code,
                                      RoutePriority priority,
                                      PjbRuntimePressureService.Snapshot pressure,
                                      PjbRuntimeDrainService.Snapshot drain,
                                      PjbLivePressureService.Snapshot live,
                                      PjbKafkaPressureService.Snapshot kafka,
                                      boolean writeSensitive,
                                      boolean expensive,
                                      String bucket,
                                      String method,
                                      String uri,
                                      String shapeKey) {
            return new Decision(true, false, 200, code, detail, bucket, priority, method, uri, shapeKey, writeSensitive, expensive, pressure, drain, live, kafka);
        }

        private static Decision reject(boolean hardRejection,
                                       int status,
                                       String code,
                                       String detail,
                                       RoutePriority priority,
                                       PjbRuntimePressureService.Snapshot pressure,
                                       PjbRuntimeDrainService.Snapshot drain,
                                       PjbLivePressureService.Snapshot live,
                                       PjbKafkaPressureService.Snapshot kafka,
                                       boolean writeSensitive,
                                       boolean expensive,
                                       String bucket,
                                       String method,
                                       String uri,
                                       String shapeKey) {
            return new Decision(false, hardRejection, status, code, detail, bucket, priority, method, uri, shapeKey, writeSensitive, expensive, pressure, drain, live, kafka);
        }
    }

    private record CachedDecision(Decision decision, Instant expiresAt) {
    }
}
