package com.tcc.pjb.backend.platform.runtime;

import com.tcc.pjb.backend.configs.datasource.PjbDatasourceBudgetProperties;
import com.tcc.pjb.backend.configs.datasource.PjbHikariPoolHardeningBeanPostProcessor;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PjbRuntimePressureService {

    private final PjbRuntimePressureProperties properties;
    private final PjbRuntimeAccelerationProperties accelerationProperties;
    private final PjbRuntimeSizingPolicy.Footprint footprint;
    private final PjbDatasourceBudgetProperties datasourceBudgetProperties;
    private final Map<String, PjbBoundedExecutorService> executors;
    private final Map<String, HikariDataSource> dataSources;
    private final ScheduledExecutorService timeoutScheduler;
    private final long startedAtEpochMillis = System.currentTimeMillis();
    private final AtomicInteger lastPressureScore = new AtomicInteger();
    private final AtomicLong lastPressureSampleAtEpochMillis = new AtomicLong();
    private final AtomicLong sustainedPressureSinceEpochMillis = new AtomicLong();
    private final AtomicInteger lastSchedulerQueueSize = new AtomicInteger();
    private final AtomicLong lastSchedulerSampleAtEpochMillis = new AtomicLong();
    private final AtomicLong sustainedSchedulerPressureSinceEpochMillis = new AtomicLong();
    private final Map<String, AtomicInteger> lastDatasourceAwaiting = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastDatasourceUsageBasisPoints = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastDatasourceSampleAtEpochMillis = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> sustainedDatasourcePressureSinceEpochMillis = new ConcurrentHashMap<>();
    private final AtomicLong lastHeapUsedMiB = new AtomicLong();
    private final AtomicLong lastDirectBufferUsedMiB = new AtomicLong();
    private final AtomicLong lastMemorySampleAtEpochMillis = new AtomicLong();
    private final AtomicLong sustainedMemoryPressureSinceEpochMillis = new AtomicLong();
    private final AtomicLong lastGcCollectionCount = new AtomicLong();
    private final AtomicLong lastGcCollectionTimeMillis = new AtomicLong();
    private final AtomicLong lastGcSampleAtEpochMillis = new AtomicLong();
    private final AtomicLong sustainedGcPressureSinceEpochMillis = new AtomicLong();
    private final AtomicLong cachedSnapshotAtEpochMillis = new AtomicLong();
    private volatile Snapshot cachedSnapshot;

    public PjbRuntimePressureService(PjbRuntimePressureProperties properties,
                                     PjbRuntimeAccelerationProperties accelerationProperties,
                                     PjbRuntimeSizingPolicy.Footprint footprint,
                                     PjbDatasourceBudgetProperties datasourceBudgetProperties,
                                     Map<String, PjbBoundedExecutorService> executors,
                                     Map<String, HikariDataSource> dataSources,
                                     @Qualifier("pjbTimeoutScheduler") ScheduledExecutorService timeoutScheduler) {
        this.properties = properties;
        this.accelerationProperties = accelerationProperties;
        this.footprint = footprint;
        this.datasourceBudgetProperties = datasourceBudgetProperties;
        this.executors = executors;
        this.dataSources = dataSources;
        this.timeoutScheduler = timeoutScheduler;
    }

    public Snapshot snapshot() {
        long now = System.currentTimeMillis();
        Snapshot cached = cachedSnapshot;
        Duration cacheTtl = properties.getSnapshotCacheTtl() == null || properties.getSnapshotCacheTtl().isNegative() ? Duration.ofMillis(250) : properties.getSnapshotCacheTtl();
        if (!cacheTtl.isZero() && cached != null && now - cachedSnapshotAtEpochMillis.get() <= cacheTtl.toMillis()) {
            return cached;
        }
        Snapshot fresh = computeSnapshot(now);
        cachedSnapshot = fresh;
        cachedSnapshotAtEpochMillis.set(now);
        return fresh;
    }

    private Snapshot computeSnapshot(long now) {
        Set<String> criticalLanes = criticalLaneNames();
        Set<String> criticalPools = criticalPoolRoles();
        List<ExecutorPressure> executorPressures = executors.entrySet().stream()
                .map(entry -> toExecutorPressure(entry.getKey(), entry.getValue(), criticalLanes))
                .sorted(Comparator.comparing(ExecutorPressure::criticalLane).reversed()
                        .thenComparing(ExecutorPressure::overloaded).reversed()
                        .thenComparing(ExecutorPressure::stressed).reversed()
                        .thenComparing(ExecutorPressure::utilizationRatio).reversed())
                .toList();
        List<DatasourcePressure> datasourcePressures = dataSources.entrySet().stream()
                .map(entry -> toDatasourcePressure(entry.getKey(), entry.getValue(), criticalPools))
                .sorted(Comparator.comparing(DatasourcePressure::criticalPool).reversed()
                        .thenComparing(DatasourcePressure::degraded).reversed()
                        .thenComparing(DatasourcePressure::risingFast).reversed()
                        .thenComparing(DatasourcePressure::usageRatio).reversed())
                .toList();
        SchedulerPressure schedulerPressure = toSchedulerPressure(timeoutScheduler);
        long uptimeMillis = uptimeMillis(now);
        boolean warmingUp = properties.isEnabled() && uptimeMillis < minimumReadyAge().toMillis();
        MemoryPressure memoryPressure = toMemoryPressure(warmingUp);
        GcPressure gcPressure = toGcPressure(warmingUp, now);
        long overloadedExecutors = executorPressures.stream().filter(ExecutorPressure::overloaded).count();
        long criticalOverloadedExecutors = executorPressures.stream().filter(executor -> executor.criticalLane() && executor.overloaded()).count();
        long degradedPools = datasourcePressures.stream().filter(DatasourcePressure::degraded).count();
        long runawayCriticalPools = datasourcePressures.stream().filter(pool -> pool.criticalPool() && (pool.risingFast() || pool.headroomTight()) && pool.degraded()).count();
        boolean schedulerDegraded = schedulerPressure != null && schedulerPressure.degraded();
        int pressureScore = computePressureScore(overloadedExecutors, criticalOverloadedExecutors, degradedPools, runawayCriticalPools, schedulerPressure, executorPressures, datasourcePressures, memoryPressure, gcPressure, warmingUp);
        PressureTrend trend = updateTrend(pressureScore, warmingUp, now);
        boolean criticalLaneFailure = properties.isEnabled()
                && properties.isFailReadyOnCriticalLaneOverload()
                && criticalOverloadedExecutors > Math.max(0, properties.getMaxCriticalLaneOverloads());
        boolean criticalDatasourceFailure = properties.isEnabled()
                && properties.isFailReadyOnCriticalDatasourceRunaway()
                && runawayCriticalPools > 0L;
        boolean criticalMemoryFailure = properties.isEnabled()
                && properties.isFailReadyOnCriticalMemoryRunaway()
                && memoryPressure.criticalRunaway();
        boolean criticalGcFailure = properties.isEnabled()
                && properties.isFailReadyOnCriticalGcPressure()
                && gcPressure.critical();
        boolean ready = !properties.isEnabled()
                || (!warmingUp
                && !criticalLaneFailure
                && !criticalDatasourceFailure
                && !criticalMemoryFailure
                && !criticalGcFailure
                && overloadedExecutors <= Math.max(0, properties.getMaxOverloadedExecutors())
                && degradedPools <= Math.max(0, properties.getMaxDegradedPools())
                && !schedulerDegraded
                && !trend.risingFast());
        int headroomScore = computeHeadroomScore(pressureScore, criticalOverloadedExecutors, degradedPools, runawayCriticalPools, schedulerPressure, trend, memoryPressure, gcPressure);
        List<PressureAlert> alerts = buildAlerts(executorPressures, datasourcePressures, schedulerPressure, memoryPressure, gcPressure, warmingUp, pressureScore, trend);
        return new Snapshot(
                ready,
                warmingUp,
                pressureScore,
                headroomScore,
                normalizeRole(accelerationProperties.getComponentRole()),
                footprint.availableProcessors(),
                footprint.maxMemoryMiB(),
                Instant.ofEpochMilli(now),
                uptimeMillis,
                executorPressures,
                datasourcePressures,
                schedulerPressure,
                memoryPressure,
                gcPressure,
                trend,
                alerts
        );
    }

    private ExecutorPressure toExecutorPressure(String beanName, PjbBoundedExecutorService executor, Set<String> criticalLanes) {
        String laneName = normalizeLane(beanName);
        boolean criticalLane = criticalLanes.contains(laneName);
        double configuredThreshold = criticalLane
                ? Math.min(properties.getExecutorUtilizationThreshold(), PjbRuntimeSizingPolicy.readinessExecutorUtilizationThreshold(accelerationProperties.getComponentRole()) * 0.95d)
                : properties.getExecutorUtilizationThreshold();
        double roleAwareThreshold = Math.min(0.995d, Math.max(0.80d, configuredThreshold));
        double averageAcquireWaitMillis = executor.averageAcquireWaitMillis();
        boolean waitPressure = averageAcquireWaitMillis >= Math.max(25.0d, properties.getExecutorAcquireWaitThresholdMillis());
        Duration recentRejectionWindow = sanitize(properties.getExecutorRecentRejectionWindow(), Duration.ofSeconds(10));
        boolean rejectedRecently = executor.millisSinceLastRejection() <= Math.max(0L, recentRejectionWindow.toMillis());
        boolean criticalRejected = criticalLane && rejectedRecently && executor.consecutiveRejections() > 0;
        boolean saturatedAfterRejection = rejectedRecently
                && executor.saturationRejections() > 0
                && executor.utilizationRatio() >= Math.max(0.70d, roleAwareThreshold * 0.85d);
        boolean overloaded = properties.isEnabled() && (executor.overloaded(roleAwareThreshold, recentRejectionWindow, properties.getExecutorConsecutiveRejectionThreshold()) || waitPressure || criticalRejected || saturatedAfterRejection);
        boolean stressed = overloaded
                || executor.utilizationRatio() >= Math.max(0.70d, roleAwareThreshold * 0.92d)
                || averageAcquireWaitMillis >= Math.max(10.0d, properties.getExecutorAcquireWaitThresholdMillis() * 0.70d)
                || criticalRejected
                || (!criticalLane && executor.consecutiveRejections() > 0);
        return new ExecutorPressure(
                beanName,
                laneName,
                executor.threadNamePrefix(),
                executor.concurrencyLimit(),
                executor.activeTasks(),
                executor.utilizationRatio(),
                averageAcquireWaitMillis,
                executor.saturationRejections(),
                executor.consecutiveRejections(),
                executor.lastRejectedAt(),
                executor.acceptingTasks(),
                criticalLane,
                overloaded,
                stressed
        );
    }

    private DatasourcePressure toDatasourcePressure(String beanName, HikariDataSource dataSource, Set<String> criticalPools) {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        int active = pool == null ? 0 : Math.max(0, pool.getActiveConnections());
        int idle = pool == null ? 0 : Math.max(0, pool.getIdleConnections());
        int awaiting = pool == null ? 0 : Math.max(0, pool.getThreadsAwaitingConnection());
        int maxPoolSize = Math.max(1, dataSource.getMaximumPoolSize());
        int budgetCeiling = PjbHikariPoolHardeningBeanPostProcessor.budgetCeiling(beanName, maxPoolSize, datasourceBudgetProperties);
        int headroom = Math.max(0, maxPoolSize - active);
        double usageRatio = active / (double) maxPoolSize;
        boolean degraded = properties.isEnabled() && (usageRatio >= Math.max(0.50d, Math.min(0.999d, properties.getDatasourceUsageThreshold())) || awaiting >= Math.max(1, properties.getDatasourceAwaitingThreshold()));
        String poolRole = normalizePoolRole(beanName);
        DatasourceTrend trend = updateDatasourceTrend(beanName, awaiting, usageRatio, degraded);
        boolean criticalPool = criticalPools.contains(poolRole);
        boolean headroomTight = criticalPool && headroom <= Math.max(1, properties.getCriticalDatasourceHeadroomThreshold());
        return new DatasourcePressure(
                beanName,
                poolRole,
                active,
                idle,
                awaiting,
                maxPoolSize,
                budgetCeiling,
                headroom,
                usageRatio,
                criticalPool,
                headroomTight,
                trend.awaitingDelta(),
                trend.usageDeltaBasisPoints() / 10_000.0d,
                trend.risingFast(),
                trend.sustained(),
                degraded
        );
    }

    private SchedulerPressure toSchedulerPressure(ScheduledExecutorService scheduler) {
        if (!(scheduler instanceof ScheduledThreadPoolExecutor executor)) {
            return new SchedulerPressure("pjbTimeoutScheduler", 0, 0, 0, 0.0d, 0, false, false, false);
        }
        int queueSize = Math.max(0, executor.getQueue().size());
        int activeThreads = Math.max(0, executor.getActiveCount());
        int poolSize = Math.max(1, executor.getPoolSize() > 0 ? executor.getPoolSize() : executor.getCorePoolSize());
        double utilizationRatio = activeThreads / (double) poolSize;
        boolean degraded = properties.isEnabled() && (queueSize >= Math.max(1, properties.getSchedulerQueueSizeThreshold())
                || (queueSize > 0 && utilizationRatio >= Math.max(0.50d, Math.min(0.999d, properties.getSchedulerActiveUtilizationThreshold()))));
        SchedulerTrend trend = updateSchedulerTrend(queueSize, degraded);
        return new SchedulerPressure("pjbTimeoutScheduler", queueSize, activeThreads, poolSize, utilizationRatio, trend.queueDelta(), trend.risingFast(), trend.sustained(), degraded);
    }

    private int computePressureScore(long overloadedExecutors,
                                     long criticalOverloadedExecutors,
                                     long degradedPools,
                                     long runawayCriticalPools,
                                     SchedulerPressure schedulerPressure,
                                     List<ExecutorPressure> executors,
                                     List<DatasourcePressure> datasources,
                                     MemoryPressure memoryPressure,
                                     GcPressure gcPressure,
                                     boolean warmingUp) {
        int score = 0;
        score += Math.min(45, (int) overloadedExecutors * 15);
        score += Math.min(20, (int) criticalOverloadedExecutors * 20);
        score += Math.min(25, (int) degradedPools * 12);
        score += Math.min(20, (int) runawayCriticalPools * 15);
        if (schedulerPressure != null && schedulerPressure.degraded()) {
            score += 20;
        }
        if (schedulerPressure != null && schedulerPressure.risingFast()) {
            score += 10;
        }
        long stressedExecutors = executors.stream().filter(ExecutorPressure::stressed).count();
        if (stressedExecutors > overloadedExecutors) {
            score += Math.min(10, (int) (stressedExecutors - overloadedExecutors) * 5);
        }
        long awaitingHeavyPools = datasources.stream().filter(ds -> ds.awaitingThreads() > 0).count();
        if (awaitingHeavyPools > degradedPools) {
            score += Math.min(10, (int) (awaitingHeavyPools - degradedPools) * 3);
        }
        long risingDatasourceCount = datasources.stream().filter(DatasourcePressure::risingFast).count();
        if (risingDatasourceCount > 0L) {
            score += Math.min(10, (int) risingDatasourceCount * 4);
        }
        if (memoryPressure.degraded()) {
            score += 15;
        }
        if (memoryPressure.heapRisingFast()) {
            score += 8;
        }
        if (memoryPressure.directBufferDeltaMiB() >= Math.max(16, properties.getDirectBufferRisingFastDeltaMiB() / 2)) {
            score += 5;
        }
        if (memoryPressure.criticalRunaway()) {
            score += 20;
        }
        if (gcPressure.degraded()) {
            score += 12;
        }
        if (gcPressure.risingFast()) {
            score += 8;
        }
        if (gcPressure.sustained()) {
            score += 6;
        }
        if (gcPressure.critical()) {
            score += 18;
        }
        if (warmingUp) {
            score = Math.max(score, 10);
        }
        return Math.min(100, score);
    }

    private int computeHeadroomScore(int pressureScore,
                                     long criticalOverloadedExecutors,
                                     long degradedPools,
                                     long runawayCriticalPools,
                                     SchedulerPressure schedulerPressure,
                                     PressureTrend trend,
                                     MemoryPressure memoryPressure,
                                     GcPressure gcPressure) {
        int headroom = Math.max(0, 100 - pressureScore);
        if (criticalOverloadedExecutors > 0L) {
            headroom = Math.max(0, headroom - 15);
        }
        if (degradedPools > 0L) {
            headroom = Math.max(0, headroom - 10);
        }
        if (runawayCriticalPools > 0L) {
            headroom = Math.max(0, headroom - 15);
        }
        if (schedulerPressure != null && schedulerPressure.degraded()) {
            headroom = Math.max(0, headroom - 10);
        }
        if (schedulerPressure != null && schedulerPressure.risingFast()) {
            headroom = Math.max(0, headroom - 8);
        }
        if (trend.risingFast()) {
            headroom = Math.max(0, headroom - 10);
        }
        if (memoryPressure.degraded()) {
            headroom = Math.max(0, headroom - 12);
        }
        if (memoryPressure.heapRisingFast()) {
            headroom = Math.max(0, headroom - 8);
        }
        if (memoryPressure.criticalRunaway()) {
            headroom = Math.max(0, headroom - 18);
        }
        if (gcPressure.degraded()) {
            headroom = Math.max(0, headroom - 10);
        }
        if (gcPressure.risingFast()) {
            headroom = Math.max(0, headroom - 8);
        }
        if (gcPressure.critical()) {
            headroom = Math.max(0, headroom - 16);
        }
        return headroom;
    }

    private PressureTrend updateTrend(int pressureScore, boolean warmingUp, long now) {
        int previousScore = lastPressureScore.getAndSet(pressureScore);
        long previousSampleAt = lastPressureSampleAtEpochMillis.getAndSet(now);
        Duration trendWindow = sanitize(properties.getPressureTrendWindow(), Duration.ofSeconds(45));
        boolean previousFresh = previousSampleAt > 0L && now - previousSampleAt <= trendWindow.toMillis();
        int delta = pressureScore - previousScore;
        boolean risingFast = properties.isEnabled()
                && !warmingUp
                && previousFresh
                && delta >= Math.max(5, properties.getPressureRisingFastDelta());
        int sustainedThreshold = Math.max(1, properties.getPressureSustainedThreshold());
        if (pressureScore >= sustainedThreshold && !warmingUp) {
            sustainedPressureSinceEpochMillis.compareAndSet(0L, now);
        } else {
            sustainedPressureSinceEpochMillis.set(0L);
        }
        long sustainedSince = sustainedPressureSinceEpochMillis.get();
        boolean sustained = sustainedSince > 0L
                && now - sustainedSince >= sanitize(properties.getPressureSustainedWindow(), Duration.ofMinutes(2)).toMillis();
        return new PressureTrend(previousScore, delta, previousFresh, risingFast, sustained, sustainedSince > 0L ? Instant.ofEpochMilli(sustainedSince) : null);
    }

    private SchedulerTrend updateSchedulerTrend(int queueSize, boolean degraded) {
        long now = System.currentTimeMillis();
        int previousQueue = lastSchedulerQueueSize.getAndSet(queueSize);
        long previousSampleAt = lastSchedulerSampleAtEpochMillis.getAndSet(now);
        Duration trendWindow = sanitize(properties.getSchedulerTrendWindow(), Duration.ofSeconds(30));
        boolean previousFresh = previousSampleAt > 0L && now - previousSampleAt <= trendWindow.toMillis();
        int delta = queueSize - previousQueue;
        boolean risingFast = properties.isEnabled()
                && previousFresh
                && delta >= Math.max(1, properties.getSchedulerQueueRisingFastDelta());
        if (degraded) {
            sustainedSchedulerPressureSinceEpochMillis.compareAndSet(0L, now);
        } else {
            sustainedSchedulerPressureSinceEpochMillis.set(0L);
        }
        long sustainedSince = sustainedSchedulerPressureSinceEpochMillis.get();
        boolean sustained = sustainedSince > 0L
                && now - sustainedSince >= sanitize(properties.getSchedulerSustainedWindow(), Duration.ofMinutes(2)).toMillis();
        return new SchedulerTrend(delta, previousFresh, risingFast, sustained, sustainedSince > 0L ? Instant.ofEpochMilli(sustainedSince) : null);
    }

    private DatasourceTrend updateDatasourceTrend(String beanName, int awaiting, double usageRatio, boolean degraded) {
        long now = System.currentTimeMillis();
        AtomicInteger awaitingHolder = lastDatasourceAwaiting.computeIfAbsent(beanName, ignored -> new AtomicInteger());
        AtomicLong usageHolder = lastDatasourceUsageBasisPoints.computeIfAbsent(beanName, ignored -> new AtomicLong());
        AtomicLong sampleHolder = lastDatasourceSampleAtEpochMillis.computeIfAbsent(beanName, ignored -> new AtomicLong());
        AtomicLong sustainedHolder = sustainedDatasourcePressureSinceEpochMillis.computeIfAbsent(beanName, ignored -> new AtomicLong());
        int previousAwaiting = awaitingHolder.getAndSet(awaiting);
        long currentUsageBasisPoints = Math.max(0L, Math.round(usageRatio * 10_000.0d));
        long previousUsageBasisPoints = usageHolder.getAndSet(currentUsageBasisPoints);
        long previousSampleAt = sampleHolder.getAndSet(now);
        Duration trendWindow = sanitize(properties.getDatasourceTrendWindow(), Duration.ofSeconds(45));
        boolean previousFresh = previousSampleAt > 0L && now - previousSampleAt <= trendWindow.toMillis();
        int awaitingDelta = awaiting - previousAwaiting;
        long usageDeltaBasisPoints = currentUsageBasisPoints - previousUsageBasisPoints;
        boolean risingFast = properties.isEnabled()
                && previousFresh
                && (awaitingDelta >= Math.max(1, properties.getDatasourceAwaitingRisingFastDelta())
                || usageDeltaBasisPoints >= Math.max(100L, Math.round(properties.getDatasourceUsageRisingFastDelta() * 10_000.0d)));
        if (degraded) {
            sustainedHolder.compareAndSet(0L, now);
        } else {
            sustainedHolder.set(0L);
        }
        long sustainedSince = sustainedHolder.get();
        boolean sustained = sustainedSince > 0L
                && now - sustainedSince >= sanitize(properties.getDatasourceSustainedWindow(), Duration.ofMinutes(2)).toMillis();
        return new DatasourceTrend(awaitingDelta, usageDeltaBasisPoints, previousFresh, risingFast, sustained, sustainedSince > 0L ? Instant.ofEpochMilli(sustainedSince) : null);
    }

    private List<PressureAlert> buildAlerts(List<ExecutorPressure> executors,
                                            List<DatasourcePressure> datasources,
                                            SchedulerPressure schedulerPressure,
                                            MemoryPressure memoryPressure,
                                            GcPressure gcPressure,
                                            boolean warmingUp,
                                            int pressureScore,
                                            PressureTrend trend) {
        List<PressureAlert> alerts = new ArrayList<>();
        if (warmingUp) {
            alerts.add(new PressureAlert("INFO", "runtime", "warming_up", "Instância ainda está no período inicial de readiness."));
        }
        executors.stream()
                .filter(ExecutorPressure::overloaded)
                .limit(4)
                .forEach(executor -> alerts.add(new PressureAlert(
                        executor.criticalLane() ? "CRITICAL" : "WARN",
                        executor.beanName(),
                        executor.criticalLane() ? "critical_lane_overloaded" : "executor_overloaded",
                        "Lane " + executor.laneName() + " está saturada com utilization=" + round(executor.utilizationRatio()) + " e acquireWaitMs=" + round(executor.averageAcquireWaitMillis())
                )));
        datasources.stream()
                .filter(DatasourcePressure::degraded)
                .limit(4)
                .forEach(dataSource -> alerts.add(new PressureAlert(
                        dataSource.criticalPool() || dataSource.headroomTight() ? "CRITICAL" : "WARN",
                        dataSource.beanName(),
                        dataSource.criticalPool() ? "critical_datasource_degraded" : "datasource_degraded",
                        "Pool " + dataSource.poolRole() + " com usage=" + round(dataSource.usageRatio()) + ", awaiting=" + dataSource.awaitingThreads() + " e headroom=" + dataSource.headroom()
                )));
        datasources.stream()
                .filter(DatasourcePressure::risingFast)
                .limit(3)
                .forEach(dataSource -> alerts.add(new PressureAlert(
                        dataSource.criticalPool() ? "CRITICAL" : "WARN",
                        dataSource.beanName(),
                        dataSource.criticalPool() ? "critical_datasource_runaway" : "datasource_rising_fast",
                        "Pool " + dataSource.poolRole() + " com awaitingDelta=" + dataSource.awaitingDelta() + " e usageDelta=" + round(dataSource.usageDelta())
                )));
        datasources.stream()
                .filter(DatasourcePressure::headroomTight)
                .limit(2)
                .forEach(dataSource -> alerts.add(new PressureAlert(
                        "CRITICAL",
                        dataSource.beanName(),
                        "critical_datasource_headroom_tight",
                        "Pool crítico " + dataSource.poolRole() + " está com headroom baixo=" + dataSource.headroom() + "/" + dataSource.maximumPoolSize()
                )));
        if (schedulerPressure != null && schedulerPressure.degraded()) {
            alerts.add(new PressureAlert(
                    schedulerPressure.risingFast() || schedulerPressure.sustained() ? "CRITICAL" : "WARN",
                    schedulerPressure.name(),
                    "scheduler_backlog",
                    "Scheduler com queueSize=" + schedulerPressure.queueSize() + " e utilization=" + round(schedulerPressure.utilizationRatio())
            ));
        }
        if (schedulerPressure != null && schedulerPressure.risingFast()) {
            alerts.add(new PressureAlert("WARN", schedulerPressure.name(), "scheduler_rising_fast", "Fila do scheduler cresceu rapidamente (delta=" + schedulerPressure.queueDelta() + ")."));
        }
        if (schedulerPressure != null && schedulerPressure.sustained()) {
            alerts.add(new PressureAlert("WARN", schedulerPressure.name(), "scheduler_sustained", "Scheduler permanece degradado com backlog sustentado."));
        }
        if (memoryPressure.degraded()) {
            alerts.add(new PressureAlert(memoryPressure.criticalRunaway() ? "CRITICAL" : "WARN", "memory", memoryPressure.criticalRunaway() ? "critical_memory_runaway" : "memory_pressure", "Heap=" + memoryPressure.heapUsedMiB() + "MiB/" + memoryPressure.heapMaxMiB() + "MiB, metaspace=" + memoryPressure.metaspaceUsedMiB() + "MiB, direct=" + memoryPressure.directBufferUsedMiB() + "MiB."));
        }
        if (memoryPressure.heapRisingFast()) {
            alerts.add(new PressureAlert("WARN", "memory", "heap_rising_fast", "Heap cresceu rapidamente (delta=" + memoryPressure.heapDeltaMiB() + "MiB)."));
        }
        if (memoryPressure.directBufferDeltaMiB() >= Math.max(16, properties.getDirectBufferRisingFastDeltaMiB())) {
            alerts.add(new PressureAlert("WARN", "memory", "direct_buffer_rising_fast", "Direct buffers cresceram rapidamente (delta=" + memoryPressure.directBufferDeltaMiB() + "MiB)."));
        }
        if (gcPressure.degraded()) {
            alerts.add(new PressureAlert(gcPressure.critical() ? "CRITICAL" : "WARN", "gc", gcPressure.critical() ? "critical_gc_pressure" : "gc_pressure", "GC com pauseRatio=" + round(gcPressure.pauseRatio()) + ", averagePauseMillis=" + round(gcPressure.averagePauseMillis()) + " e collectionTimeDeltaMillis=" + gcPressure.collectionTimeDeltaMillis() + "."));
        }
        if (gcPressure.risingFast()) {
            alerts.add(new PressureAlert("WARN", "gc", "gc_rising_fast", "Tempo de GC cresceu rapidamente (delta=" + gcPressure.collectionTimeDeltaMillis() + "ms)."));
        }
        if (gcPressure.sustained()) {
            alerts.add(new PressureAlert("WARN", "gc", "gc_sustained", "GC permanece degradado com pressão sustentada."));
        }
        if (trend.risingFast()) {
            alerts.add(new PressureAlert("WARN", "runtime", "pressure_rising_fast", "Score operacional subiu rapidamente para " + pressureScore + " (delta=" + trend.deltaScore() + ")."));
        }
        if (trend.sustained()) {
            alerts.add(new PressureAlert("WARN", "runtime", "pressure_sustained", "Score operacional permanece elevado desde " + trend.sustainedSince() + "."));
        }
        if (pressureScore >= 80) {
            alerts.add(new PressureAlert("CRITICAL", "runtime", "pressure_score", "Score operacional crítico=" + pressureScore));
        } else if (pressureScore >= 50) {
            alerts.add(new PressureAlert("WARN", "runtime", "pressure_score", "Score operacional elevado=" + pressureScore));
        }
        return alerts;
    }

    private Set<String> criticalLaneNames() {
        LinkedHashSet<String> lanes = new LinkedHashSet<>();
        if (properties.getCriticalLanes() != null) {
            properties.getCriticalLanes().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(this::normalizeLane)
                    .forEach(lanes::add);
        }
        if (!lanes.isEmpty()) {
            return lanes;
        }
        return switch (normalizeRole(accelerationProperties.getComponentRole())) {
            case "api" -> Set.of("external-io", "live", "io");
            case "worker" -> Set.of("job", "io");
            default -> Set.of("external-io", "live", "job");
        };
    }

    private Set<String> criticalPoolRoles() {
        return switch (normalizeRole(accelerationProperties.getComponentRole())) {
            case "api" -> Set.of("write", "read");
            case "worker" -> Set.of("write");
            default -> Set.of("write", "read");
        };
    }

    private GcPressure toGcPressure(boolean warmingUp, long now) {
        long totalCollections = 0L;
        long totalCollectionTimeMillis = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = bean.getCollectionCount();
            long time = bean.getCollectionTime();
            if (count > 0L) {
                totalCollections += count;
            }
            if (time > 0L) {
                totalCollectionTimeMillis += time;
            }
        }
        long previousCollections = lastGcCollectionCount.getAndSet(totalCollections);
        long previousCollectionTimeMillis = lastGcCollectionTimeMillis.getAndSet(totalCollectionTimeMillis);
        long previousSampleAt = lastGcSampleAtEpochMillis.getAndSet(now);
        Duration trendWindow = sanitize(properties.getGcTrendWindow(), Duration.ofSeconds(45));
        boolean previousFresh = previousSampleAt > 0L && now - previousSampleAt <= trendWindow.toMillis();
        long sampleWindowMillis = previousFresh ? Math.max(1L, now - previousSampleAt) : 0L;
        long collectionCountDelta = Math.max(0L, totalCollections - previousCollections);
        long collectionTimeDeltaMillis = Math.max(0L, totalCollectionTimeMillis - previousCollectionTimeMillis);
        double pauseRatio = sampleWindowMillis <= 0L ? 0.0d : Math.min(1.0d, collectionTimeDeltaMillis / (double) sampleWindowMillis);
        double averagePauseMillis = collectionCountDelta <= 0L ? 0.0d : collectionTimeDeltaMillis / (double) collectionCountDelta;
        boolean degraded = properties.isEnabled() && !warmingUp && previousFresh && (pauseRatio >= Math.max(0.01d, properties.getGcPauseRatioThreshold())
                || averagePauseMillis >= Math.max(5.0d, properties.getGcAveragePauseMillisThreshold()));
        boolean risingFast = properties.isEnabled() && !warmingUp && previousFresh && collectionTimeDeltaMillis >= Math.max(10L, properties.getGcCollectionTimeRisingFastMillis());
        if (degraded) {
            sustainedGcPressureSinceEpochMillis.compareAndSet(0L, now);
        } else {
            sustainedGcPressureSinceEpochMillis.set(0L);
        }
        long sustainedSince = sustainedGcPressureSinceEpochMillis.get();
        boolean sustained = sustainedSince > 0L && now - sustainedSince >= sanitize(properties.getGcSustainedWindow(), Duration.ofMinutes(2)).toMillis();
        boolean critical = properties.isEnabled() && !warmingUp && degraded && (pauseRatio >= Math.min(0.99d, properties.getGcPauseRatioThreshold() + 0.05d)
                || averagePauseMillis >= properties.getGcAveragePauseMillisThreshold() * 1.35d
                || risingFast
                || sustained);
        return new GcPressure(totalCollections, totalCollectionTimeMillis, collectionCountDelta, collectionTimeDeltaMillis, pauseRatio, averagePauseMillis, previousFresh, risingFast, sustained, critical, degraded);
    }

    private MemoryPressure toMemoryPressure(boolean warmingUp) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        long heapUsedMiB = toMiB(heapUsage.getUsed());
        long heapMaxMiB = Math.max(1L, toMiB(heapUsage.getMax() > 0L ? heapUsage.getMax() : Math.max(heapUsage.getCommitted(), footprint.maxMemoryMiB() * 1024L * 1024L)));
        double heapUsageRatio = heapUsedMiB / (double) heapMaxMiB;
        MemoryPoolMXBean metaspace = ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == MemoryType.NON_HEAP)
                .filter(pool -> pool.getName() != null && pool.getName().toLowerCase(Locale.ROOT).contains("metaspace"))
                .findFirst()
                .orElse(null);
        long metaspaceUsedMiB = metaspace == null ? 0L : toMiB(metaspace.getUsage().getUsed());
        long metaspaceMaxMiB = metaspace == null ? 0L : Math.max(1L, toMiB(metaspace.getUsage().getMax() > 0L ? metaspace.getUsage().getMax() : metaspace.getUsage().getCommitted()));
        double metaspaceUsageRatio = metaspaceMaxMiB <= 0L ? 0.0d : metaspaceUsedMiB / (double) metaspaceMaxMiB;
        BufferPoolMXBean directBufferPool = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).stream()
                .filter(pool -> pool.getName() != null && pool.getName().equalsIgnoreCase("direct"))
                .findFirst()
                .orElse(null);
        long directBufferUsedMiB = directBufferPool == null ? 0L : toMiB(directBufferPool.getMemoryUsed());
        double directBufferUsageRatio = footprint.maxMemoryMiB() <= 0L ? 0.0d : directBufferUsedMiB / (double) Math.max(1L, footprint.maxMemoryMiB());
        MemoryTrend memoryTrend = updateMemoryTrend(heapUsedMiB, directBufferUsedMiB, heapUsageRatio, metaspaceUsageRatio, directBufferUsageRatio, warmingUp);
        boolean degraded = properties.isEnabled() && (heapUsageRatio >= Math.max(0.50d, properties.getHeapUsageThreshold())
                || metaspaceUsageRatio >= Math.max(0.50d, properties.getMetaspaceUsageThreshold())
                || directBufferUsageRatio >= Math.max(0.03d, properties.getDirectBufferUsageThreshold()));
        boolean criticalRunaway = properties.isEnabled() && !warmingUp && degraded && (memoryTrend.heapRisingFast()
                || memoryTrend.directBufferRisingFast()
                || memoryTrend.sustained()
                || heapUsageRatio >= Math.min(0.99d, properties.getHeapUsageThreshold() + 0.05d)
                || directBufferUsageRatio >= Math.min(0.95d, properties.getDirectBufferUsageThreshold() + 0.05d));
        return new MemoryPressure(heapUsedMiB, heapMaxMiB, heapUsageRatio, memoryTrend.heapDeltaMiB(), memoryTrend.heapRisingFast(), memoryTrend.sustained(), metaspaceUsedMiB, metaspaceMaxMiB, metaspaceUsageRatio, directBufferUsedMiB, memoryTrend.directBufferDeltaMiB(), directBufferUsageRatio, degraded, criticalRunaway);
    }

    private MemoryTrend updateMemoryTrend(long heapUsedMiB,
                                          long directBufferUsedMiB,
                                          double heapUsageRatio,
                                          double metaspaceUsageRatio,
                                          double directBufferUsageRatio,
                                          boolean warmingUp) {
        long now = System.currentTimeMillis();
        long previousHeapUsed = lastHeapUsedMiB.getAndSet(heapUsedMiB);
        long previousDirectUsed = lastDirectBufferUsedMiB.getAndSet(directBufferUsedMiB);
        long previousSampleAt = lastMemorySampleAtEpochMillis.getAndSet(now);
        Duration trendWindow = sanitize(properties.getMemoryTrendWindow(), Duration.ofSeconds(45));
        boolean previousFresh = previousSampleAt > 0L && now - previousSampleAt <= trendWindow.toMillis();
        long heapDeltaMiB = heapUsedMiB - previousHeapUsed;
        long directDeltaMiB = directBufferUsedMiB - previousDirectUsed;
        boolean heapRisingFast = properties.isEnabled() && !warmingUp && previousFresh && heapDeltaMiB >= Math.max(16, properties.getHeapRisingFastDeltaMiB());
        boolean directRisingFast = properties.isEnabled() && !warmingUp && previousFresh && directDeltaMiB >= Math.max(8, properties.getDirectBufferRisingFastDeltaMiB());
        boolean degraded = heapUsageRatio >= Math.max(0.50d, properties.getHeapUsageThreshold())
                || metaspaceUsageRatio >= Math.max(0.50d, properties.getMetaspaceUsageThreshold())
                || directBufferUsageRatio >= Math.max(0.03d, properties.getDirectBufferUsageThreshold());
        if (degraded && !warmingUp) {
            sustainedMemoryPressureSinceEpochMillis.compareAndSet(0L, now);
        } else {
            sustainedMemoryPressureSinceEpochMillis.set(0L);
        }
        long sustainedSince = sustainedMemoryPressureSinceEpochMillis.get();
        boolean sustained = sustainedSince > 0L
                && now - sustainedSince >= sanitize(properties.getMemorySustainedWindow(), Duration.ofMinutes(2)).toMillis();
        return new MemoryTrend(heapDeltaMiB, directDeltaMiB, previousFresh, heapRisingFast, directRisingFast, sustained, sustainedSince > 0L ? Instant.ofEpochMilli(sustainedSince) : null);
    }

    private long toMiB(long bytes) {
        if (bytes <= 0L) {
            return 0L;
        }
        return Math.max(1L, bytes / (1024L * 1024L));
    }

    private Duration minimumReadyAge() {
        Duration configured = properties.getMinimumReadyAge();
        if (configured == null || configured.isNegative()) {
            return Duration.ofSeconds(20);
        }
        return configured;
    }

    private Duration sanitize(Duration configured, Duration fallback) {
        if (configured == null || configured.isNegative() || configured.isZero()) {
            return fallback;
        }
        return configured;
    }

    private long uptimeMillis() {
        return uptimeMillis(System.currentTimeMillis());
    }

    private long uptimeMillis(long now) {
        return Math.max(0L, now - startedAtEpochMillis);
    }

    private String normalizePoolRole(String beanName) {
        String normalized = beanName == null ? "auxiliary" : beanName.toLowerCase(Locale.ROOT);
        if (normalized.contains("read")) {
            return "read";
        }
        if (normalized.contains("write") || normalized.contains("primary")) {
            return "write";
        }
        return "auxiliary";
    }

    private String normalizeLane(String beanName) {
        String normalized = beanName == null ? "default" : beanName.toLowerCase(Locale.ROOT).trim();
        if (normalized.contains("external")) {
            return "external-io";
        }
        if (normalized.contains("burst")) {
            return "burst";
        }
        if (normalized.contains("live")) {
            return "live";
        }
        if (normalized.contains("job")) {
            return "job";
        }
        if (normalized.contains("io")) {
            return "io";
        }
        if (normalized.contains("async")) {
            return "async";
        }
        return normalized;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "mixed";
        }
        String normalized = role.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "api", "worker", "mixed" -> normalized;
            default -> "mixed";
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    public record Snapshot(boolean ready,
                           boolean warmingUp,
                           int pressureScore,
                           int headroomScore,
                           String componentRole,
                           int availableProcessors,
                           long maxMemoryMiB,
                           Instant generatedAt,
                           long uptimeMillis,
                           List<ExecutorPressure> executors,
                           List<DatasourcePressure> dataSources,
                           SchedulerPressure scheduler,
                           MemoryPressure memory,
                           GcPressure gc,
                           PressureTrend trend,
                           List<PressureAlert> alerts) {
        public List<String> overloadedExecutorNames() {
            return executors.stream().filter(ExecutorPressure::overloaded).map(ExecutorPressure::beanName).collect(Collectors.toList());
        }

        public List<String> degradedDatasourceNames() {
            return dataSources.stream().filter(DatasourcePressure::degraded).map(DatasourcePressure::beanName).collect(Collectors.toList());
        }

        public List<String> risingDatasourceNames() {
            return dataSources.stream().filter(DatasourcePressure::risingFast).map(DatasourcePressure::beanName).collect(Collectors.toList());
        }

        public List<String> criticalTightDatasourceNames() {
            return dataSources.stream().filter(DatasourcePressure::headroomTight).map(DatasourcePressure::beanName).collect(Collectors.toList());
        }

        public List<String> criticalOverloadedExecutorNames() {
            return executors.stream().filter(executor -> executor.criticalLane() && executor.overloaded()).map(ExecutorPressure::beanName).collect(Collectors.toList());
        }

        public List<String> overloadedLaneNames() {
            return executors.stream()
                    .filter(ExecutorPressure::overloaded)
                    .map(ExecutorPressure::laneName)
                    .map(Snapshot::normalizeLane)
                    .distinct()
                    .collect(Collectors.toList());
        }

        public long overloadedLaneCount(List<String> lanes) {
            if (lanes == null || lanes.isEmpty()) {
                return 0L;
            }
            Set<String> normalized = lanes.stream()
                    .map(Snapshot::normalizeLane)
                    .collect(Collectors.toSet());
            return executors.stream()
                    .filter(ExecutorPressure::overloaded)
                    .map(ExecutorPressure::laneName)
                    .map(Snapshot::normalizeLane)
                    .filter(normalized::contains)
                    .distinct()
                    .count();
        }

        public boolean hasOverloadedLane(String laneName) {
            String normalized = normalizeLane(laneName);
            return executors.stream()
                    .filter(ExecutorPressure::overloaded)
                    .map(ExecutorPressure::laneName)
                    .map(Snapshot::normalizeLane)
                    .anyMatch(normalized::equals);
        }

        public boolean hasCriticalOverloadedLane(String laneName) {
            String normalized = normalizeLane(laneName);
            return executors.stream()
                    .filter(executor -> executor.criticalLane() && executor.overloaded())
                    .map(ExecutorPressure::laneName)
                    .map(Snapshot::normalizeLane)
                    .anyMatch(normalized::equals);
        }

        public double averageLaneUtilization(List<String> lanes) {
            if (lanes == null || lanes.isEmpty()) {
                return 0.0d;
            }
            Set<String> normalized = lanes.stream()
                    .map(Snapshot::normalizeLane)
                    .collect(Collectors.toSet());
            List<ExecutorPressure> matching = executors.stream()
                    .filter(executor -> normalized.contains(normalizeLane(executor.laneName())))
                    .toList();
            if (matching.isEmpty()) {
                return 0.0d;
            }
            return matching.stream().mapToDouble(ExecutorPressure::utilizationRatio).average().orElse(0.0d);
        }

        public int laneHeadroomScore(List<String> lanes) {
            double averageUtilization = averageLaneUtilization(lanes);
            int score = Math.max(0, 100 - (int) Math.round(averageUtilization * 100.0d));
            long hotspots = overloadedLaneCount(lanes);
            if (hotspots > 0L) {
                score = Math.max(0, score - (int) Math.min(40L, hotspots * 20L));
            }
            return score;
        }

        public boolean laneBudgetBreached(List<String> lanes,
                                          double averageUtilizationThreshold,
                                          int headroomScoreThreshold) {
            return averageLaneUtilization(lanes) >= Math.max(0.01d, averageUtilizationThreshold)
                    || laneHeadroomScore(lanes) <= Math.max(0, headroomScoreThreshold);
        }

        public boolean schedulerTrendingUp() {
            return scheduler != null && scheduler.risingFast();
        }

        public boolean schedulerSustainedPressure() {
            return scheduler != null && scheduler.sustained();
        }

        public boolean criticalDatasourceRunaway() {
            return dataSources.stream().anyMatch(pool -> pool.criticalPool() && pool.degraded() && (pool.risingFast() || pool.headroomTight()));
        }

        public boolean criticalMemoryRunaway() {
            return memory != null && memory.criticalRunaway();
        }

        public boolean criticalGcPressure() {
            return gc != null && gc.critical();
        }

        private static String normalizeLane(String laneName) {
            if (laneName == null || laneName.isBlank()) {
                return "default";
            }
            String normalized = laneName.toLowerCase(Locale.ROOT).trim();
            return switch (normalized) {
                case "externalio", "external_io", "external-io" -> "external-io";
                default -> normalized.replace('_', '-');
            };
        }
    }

    public record ExecutorPressure(String beanName,
                                   String laneName,
                                   String threadNamePrefix,
                                   int concurrencyLimit,
                                   int activeTasks,
                                   double utilizationRatio,
                                   double averageAcquireWaitMillis,
                                   long saturationRejections,
                                   int consecutiveRejections,
                                   Instant lastRejectedAt,
                                   boolean acceptingTasks,
                                   boolean criticalLane,
                                   boolean overloaded,
                                   boolean stressed) {
    }

    public record DatasourcePressure(String beanName,
                                     String poolRole,
                                     int activeConnections,
                                     int idleConnections,
                                     int awaitingThreads,
                                     int maximumPoolSize,
                                     int budgetCeiling,
                                     int headroom,
                                     double usageRatio,
                                     boolean criticalPool,
                                     boolean headroomTight,
                                     int awaitingDelta,
                                     double usageDelta,
                                     boolean risingFast,
                                     boolean sustained,
                                     boolean degraded) {
    }

    public record SchedulerPressure(String name,
                                    int queueSize,
                                    int activeThreads,
                                    int poolSize,
                                    double utilizationRatio,
                                    int queueDelta,
                                    boolean risingFast,
                                    boolean sustained,
                                    boolean degraded) {
    }

    public record PressureTrend(int previousScore,
                                int deltaScore,
                                boolean previousSampleFresh,
                                boolean risingFast,
                                boolean sustained,
                                Instant sustainedSince) {
    }

    public record DatasourceTrend(int awaitingDelta,
                                  long usageDeltaBasisPoints,
                                  boolean previousSampleFresh,
                                  boolean risingFast,
                                  boolean sustained,
                                  Instant sustainedSince) {
    }

    public record SchedulerTrend(int queueDelta,
                                 boolean previousSampleFresh,
                                 boolean risingFast,
                                 boolean sustained,
                                 Instant sustainedSince) {
    }

    public record MemoryTrend(long heapDeltaMiB,
                              long directBufferDeltaMiB,
                              boolean previousSampleFresh,
                              boolean heapRisingFast,
                              boolean directBufferRisingFast,
                              boolean sustained,
                              Instant sustainedSince) {
    }

    public record MemoryPressure(long heapUsedMiB,
                                 long heapMaxMiB,
                                 double heapUsageRatio,
                                 long heapDeltaMiB,
                                 boolean heapRisingFast,
                                 boolean heapSustained,
                                 long metaspaceUsedMiB,
                                 long metaspaceMaxMiB,
                                 double metaspaceUsageRatio,
                                 long directBufferUsedMiB,
                                 long directBufferDeltaMiB,
                                 double directBufferUsageRatio,
                                 boolean degraded,
                                 boolean criticalRunaway) {
    }

    public record GcPressure(long totalCollections,
                             long totalCollectionTimeMillis,
                             long collectionCountDelta,
                             long collectionTimeDeltaMillis,
                             double pauseRatio,
                             double averagePauseMillis,
                             boolean previousSampleFresh,
                             boolean risingFast,
                             boolean sustained,
                             boolean critical,
                             boolean degraded) {
    }

    public record PressureAlert(String severity,
                                String source,
                                String code,
                                String message) {
    }
}
