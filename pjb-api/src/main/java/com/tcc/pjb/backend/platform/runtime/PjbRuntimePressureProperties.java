package com.tcc.pjb.backend.platform.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.runtime.pressure")
public class PjbRuntimePressureProperties {

    private boolean enabled = true;
    private Duration snapshotCacheTtl = Duration.ofMillis(250);
    private Duration minimumReadyAge = Duration.ofSeconds(20);
    private double executorUtilizationThreshold = 0.96d;
    private double executorAcquireWaitThresholdMillis = 250.0d;
    private Duration executorRecentRejectionWindow = Duration.ofSeconds(60);
    private int executorConsecutiveRejectionThreshold = 2;
    private int maxOverloadedExecutors = 0;
    private boolean failReadyOnCriticalLaneOverload = true;
    private int maxCriticalLaneOverloads = 0;
    private final List<String> criticalLanes = new ArrayList<>();
    private Duration pressureTrendWindow = Duration.ofSeconds(45);
    private int pressureRisingFastDelta = 20;
    private int pressureSustainedThreshold = 45;
    private Duration pressureSustainedWindow = Duration.ofMinutes(2);
    private double datasourceUsageThreshold = 0.92d;
    private int datasourceAwaitingThreshold = 4;
    private Duration datasourceTrendWindow = Duration.ofSeconds(45);
    private int datasourceAwaitingRisingFastDelta = 3;
    private double datasourceUsageRisingFastDelta = 0.12d;
    private Duration datasourceSustainedWindow = Duration.ofMinutes(2);
    private int criticalDatasourceHeadroomThreshold = 2;
    private boolean failReadyOnCriticalDatasourceRunaway = true;
    private int maxDegradedPools = 0;
    private int schedulerQueueSizeThreshold = 200;
    private double schedulerActiveUtilizationThreshold = 0.90d;
    private Duration schedulerTrendWindow = Duration.ofSeconds(30);
    private int schedulerQueueRisingFastDelta = 40;
    private Duration schedulerSustainedWindow = Duration.ofMinutes(2);
    private double heapUsageThreshold = 0.90d;
    private double metaspaceUsageThreshold = 0.90d;
    private double directBufferUsageThreshold = 0.12d;
    private Duration memoryTrendWindow = Duration.ofSeconds(45);
    private int heapRisingFastDeltaMiB = 192;
    private int directBufferRisingFastDeltaMiB = 64;
    private Duration memorySustainedWindow = Duration.ofMinutes(2);
    private boolean failReadyOnCriticalMemoryRunaway = true;
    private Duration gcTrendWindow = Duration.ofSeconds(45);
    private Duration gcSustainedWindow = Duration.ofMinutes(2);
    private double gcPauseRatioThreshold = 0.08d;
    private double gcAveragePauseMillisThreshold = 150.0d;
    private long gcCollectionTimeRisingFastMillis = 250L;
    private boolean failReadyOnCriticalGcPressure = true;
    private Duration transactionLongRunningThreshold = Duration.ofSeconds(8);
    private Duration transactionCriticalDurationThreshold = Duration.ofSeconds(20);
    private int transactionActiveThreshold = 24;
    private int transactionBudgetViolationThreshold = 1;
    private int criticalTransactionBudgetViolationThreshold = 1;
    private boolean failReadyOnCriticalTransactionPressure = false;
    private boolean failReadyOnCriticalTransactionBudgetViolation = false;
    private final List<String> liveNamespaces = new ArrayList<>(List.of("ui-history", "ui-accessibility", "ui-presentation", "secretariat", "julgamento-votos"));
    private long liveTotalSubscribersThreshold = 2_000L;
    private long liveActiveTopicsThreshold = 400L;
    private long liveSubscriberRisingFastDelta = 250L;
    private Duration liveTrendWindow = Duration.ofSeconds(45);
    private Duration liveSustainedWindow = Duration.ofMinutes(2);
    private boolean failReadyOnCriticalLiveSurge = false;
    private double kafkaBufferAvailableRatioThreshold = 0.20d;
    private double kafkaRequestsInFlightThreshold = 150.0d;
    private double kafkaRecordQueueTimeAvgThresholdMillis = 50.0d;
    private double kafkaRecordErrorRateThreshold = 0.02d;
    private boolean failReadyOnCriticalKafkaPressure = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Duration getSnapshotCacheTtl() { return snapshotCacheTtl; }
    public void setSnapshotCacheTtl(Duration snapshotCacheTtl) { this.snapshotCacheTtl = snapshotCacheTtl == null || snapshotCacheTtl.isNegative() ? Duration.ofMillis(250) : snapshotCacheTtl; }
    public Duration getMinimumReadyAge() { return minimumReadyAge; }
    public void setMinimumReadyAge(Duration minimumReadyAge) { this.minimumReadyAge = minimumReadyAge == null || minimumReadyAge.isNegative() ? Duration.ofSeconds(20) : minimumReadyAge; }
    public double getExecutorUtilizationThreshold() { return executorUtilizationThreshold; }
    public void setExecutorUtilizationThreshold(double executorUtilizationThreshold) { this.executorUtilizationThreshold = clampRatio(executorUtilizationThreshold, 0.96d); }
    public double getExecutorAcquireWaitThresholdMillis() { return executorAcquireWaitThresholdMillis; }
    public void setExecutorAcquireWaitThresholdMillis(double executorAcquireWaitThresholdMillis) { this.executorAcquireWaitThresholdMillis = Math.max(10.0d, executorAcquireWaitThresholdMillis); }
    public Duration getExecutorRecentRejectionWindow() { return executorRecentRejectionWindow; }
    public void setExecutorRecentRejectionWindow(Duration executorRecentRejectionWindow) { this.executorRecentRejectionWindow = normalizeDuration(executorRecentRejectionWindow, Duration.ofSeconds(60)); }
    public int getExecutorConsecutiveRejectionThreshold() { return executorConsecutiveRejectionThreshold; }
    public void setExecutorConsecutiveRejectionThreshold(int executorConsecutiveRejectionThreshold) { this.executorConsecutiveRejectionThreshold = Math.max(1, executorConsecutiveRejectionThreshold); }
    public int getMaxOverloadedExecutors() { return maxOverloadedExecutors; }
    public void setMaxOverloadedExecutors(int maxOverloadedExecutors) { this.maxOverloadedExecutors = Math.max(0, maxOverloadedExecutors); }
    public boolean isFailReadyOnCriticalLaneOverload() { return failReadyOnCriticalLaneOverload; }
    public void setFailReadyOnCriticalLaneOverload(boolean failReadyOnCriticalLaneOverload) { this.failReadyOnCriticalLaneOverload = failReadyOnCriticalLaneOverload; }
    public int getMaxCriticalLaneOverloads() { return maxCriticalLaneOverloads; }
    public void setMaxCriticalLaneOverloads(int maxCriticalLaneOverloads) { this.maxCriticalLaneOverloads = Math.max(0, maxCriticalLaneOverloads); }
    public List<String> getCriticalLanes() { return criticalLanes; }
    public Duration getPressureTrendWindow() { return pressureTrendWindow; }
    public void setPressureTrendWindow(Duration pressureTrendWindow) { this.pressureTrendWindow = normalizeDuration(pressureTrendWindow, Duration.ofSeconds(45)); }
    public int getPressureRisingFastDelta() { return pressureRisingFastDelta; }
    public void setPressureRisingFastDelta(int pressureRisingFastDelta) { this.pressureRisingFastDelta = clampScore(pressureRisingFastDelta, 20); }
    public int getPressureSustainedThreshold() { return pressureSustainedThreshold; }
    public void setPressureSustainedThreshold(int pressureSustainedThreshold) { this.pressureSustainedThreshold = clampScore(pressureSustainedThreshold, 45); }
    public Duration getPressureSustainedWindow() { return pressureSustainedWindow; }
    public void setPressureSustainedWindow(Duration pressureSustainedWindow) { this.pressureSustainedWindow = normalizeDuration(pressureSustainedWindow, Duration.ofMinutes(2)); }
    public double getDatasourceUsageThreshold() { return datasourceUsageThreshold; }
    public void setDatasourceUsageThreshold(double datasourceUsageThreshold) { this.datasourceUsageThreshold = clampRatio(datasourceUsageThreshold, 0.92d); }
    public int getDatasourceAwaitingThreshold() { return datasourceAwaitingThreshold; }
    public void setDatasourceAwaitingThreshold(int datasourceAwaitingThreshold) { this.datasourceAwaitingThreshold = Math.max(1, datasourceAwaitingThreshold); }
    public Duration getDatasourceTrendWindow() { return datasourceTrendWindow; }
    public void setDatasourceTrendWindow(Duration datasourceTrendWindow) { this.datasourceTrendWindow = normalizeDuration(datasourceTrendWindow, Duration.ofSeconds(45)); }
    public int getDatasourceAwaitingRisingFastDelta() { return datasourceAwaitingRisingFastDelta; }
    public void setDatasourceAwaitingRisingFastDelta(int datasourceAwaitingRisingFastDelta) { this.datasourceAwaitingRisingFastDelta = Math.max(1, datasourceAwaitingRisingFastDelta); }
    public double getDatasourceUsageRisingFastDelta() { return datasourceUsageRisingFastDelta; }
    public void setDatasourceUsageRisingFastDelta(double datasourceUsageRisingFastDelta) { this.datasourceUsageRisingFastDelta = clampRatio(datasourceUsageRisingFastDelta, 0.12d); }
    public Duration getDatasourceSustainedWindow() { return datasourceSustainedWindow; }
    public void setDatasourceSustainedWindow(Duration datasourceSustainedWindow) { this.datasourceSustainedWindow = normalizeDuration(datasourceSustainedWindow, Duration.ofMinutes(2)); }
    public int getCriticalDatasourceHeadroomThreshold() { return criticalDatasourceHeadroomThreshold; }
    public void setCriticalDatasourceHeadroomThreshold(int criticalDatasourceHeadroomThreshold) { this.criticalDatasourceHeadroomThreshold = Math.max(0, criticalDatasourceHeadroomThreshold); }
    public boolean isFailReadyOnCriticalDatasourceRunaway() { return failReadyOnCriticalDatasourceRunaway; }
    public void setFailReadyOnCriticalDatasourceRunaway(boolean failReadyOnCriticalDatasourceRunaway) { this.failReadyOnCriticalDatasourceRunaway = failReadyOnCriticalDatasourceRunaway; }
    public int getMaxDegradedPools() { return maxDegradedPools; }
    public void setMaxDegradedPools(int maxDegradedPools) { this.maxDegradedPools = Math.max(0, maxDegradedPools); }
    public int getSchedulerQueueSizeThreshold() { return schedulerQueueSizeThreshold; }
    public void setSchedulerQueueSizeThreshold(int schedulerQueueSizeThreshold) { this.schedulerQueueSizeThreshold = Math.max(1, schedulerQueueSizeThreshold); }
    public double getSchedulerActiveUtilizationThreshold() { return schedulerActiveUtilizationThreshold; }
    public void setSchedulerActiveUtilizationThreshold(double schedulerActiveUtilizationThreshold) { this.schedulerActiveUtilizationThreshold = clampRatio(schedulerActiveUtilizationThreshold, 0.90d); }
    public Duration getSchedulerTrendWindow() { return schedulerTrendWindow; }
    public void setSchedulerTrendWindow(Duration schedulerTrendWindow) { this.schedulerTrendWindow = normalizeDuration(schedulerTrendWindow, Duration.ofSeconds(30)); }
    public int getSchedulerQueueRisingFastDelta() { return schedulerQueueRisingFastDelta; }
    public void setSchedulerQueueRisingFastDelta(int schedulerQueueRisingFastDelta) { this.schedulerQueueRisingFastDelta = Math.max(1, schedulerQueueRisingFastDelta); }
    public Duration getSchedulerSustainedWindow() { return schedulerSustainedWindow; }
    public void setSchedulerSustainedWindow(Duration schedulerSustainedWindow) { this.schedulerSustainedWindow = normalizeDuration(schedulerSustainedWindow, Duration.ofMinutes(2)); }
    public double getHeapUsageThreshold() { return heapUsageThreshold; }
    public void setHeapUsageThreshold(double heapUsageThreshold) { this.heapUsageThreshold = clampRatio(heapUsageThreshold, 0.90d); }
    public double getMetaspaceUsageThreshold() { return metaspaceUsageThreshold; }
    public void setMetaspaceUsageThreshold(double metaspaceUsageThreshold) { this.metaspaceUsageThreshold = clampRatio(metaspaceUsageThreshold, 0.90d); }
    public double getDirectBufferUsageThreshold() { return directBufferUsageThreshold; }
    public void setDirectBufferUsageThreshold(double directBufferUsageThreshold) { this.directBufferUsageThreshold = clampRatio(directBufferUsageThreshold, 0.12d); }
    public Duration getMemoryTrendWindow() { return memoryTrendWindow; }
    public void setMemoryTrendWindow(Duration memoryTrendWindow) { this.memoryTrendWindow = normalizeDuration(memoryTrendWindow, Duration.ofSeconds(45)); }
    public int getHeapRisingFastDeltaMiB() { return heapRisingFastDeltaMiB; }
    public void setHeapRisingFastDeltaMiB(int heapRisingFastDeltaMiB) { this.heapRisingFastDeltaMiB = Math.max(16, heapRisingFastDeltaMiB); }
    public int getDirectBufferRisingFastDeltaMiB() { return directBufferRisingFastDeltaMiB; }
    public void setDirectBufferRisingFastDeltaMiB(int directBufferRisingFastDeltaMiB) { this.directBufferRisingFastDeltaMiB = Math.max(8, directBufferRisingFastDeltaMiB); }
    public Duration getMemorySustainedWindow() { return memorySustainedWindow; }
    public void setMemorySustainedWindow(Duration memorySustainedWindow) { this.memorySustainedWindow = normalizeDuration(memorySustainedWindow, Duration.ofMinutes(2)); }
    public boolean isFailReadyOnCriticalMemoryRunaway() { return failReadyOnCriticalMemoryRunaway; }
    public void setFailReadyOnCriticalMemoryRunaway(boolean failReadyOnCriticalMemoryRunaway) { this.failReadyOnCriticalMemoryRunaway = failReadyOnCriticalMemoryRunaway; }
    public Duration getGcTrendWindow() { return gcTrendWindow; }
    public void setGcTrendWindow(Duration gcTrendWindow) { this.gcTrendWindow = normalizeDuration(gcTrendWindow, Duration.ofSeconds(45)); }
    public Duration getGcSustainedWindow() { return gcSustainedWindow; }
    public void setGcSustainedWindow(Duration gcSustainedWindow) { this.gcSustainedWindow = normalizeDuration(gcSustainedWindow, Duration.ofMinutes(2)); }
    public double getGcPauseRatioThreshold() { return gcPauseRatioThreshold; }
    public void setGcPauseRatioThreshold(double gcPauseRatioThreshold) { this.gcPauseRatioThreshold = clampRatio(gcPauseRatioThreshold, 0.08d); }
    public double getGcAveragePauseMillisThreshold() { return gcAveragePauseMillisThreshold; }
    public void setGcAveragePauseMillisThreshold(double gcAveragePauseMillisThreshold) { this.gcAveragePauseMillisThreshold = Math.max(10.0d, gcAveragePauseMillisThreshold); }
    public long getGcCollectionTimeRisingFastMillis() { return gcCollectionTimeRisingFastMillis; }
    public void setGcCollectionTimeRisingFastMillis(long gcCollectionTimeRisingFastMillis) { this.gcCollectionTimeRisingFastMillis = Math.max(10L, gcCollectionTimeRisingFastMillis); }
    public boolean isFailReadyOnCriticalGcPressure() { return failReadyOnCriticalGcPressure; }
    public void setFailReadyOnCriticalGcPressure(boolean failReadyOnCriticalGcPressure) { this.failReadyOnCriticalGcPressure = failReadyOnCriticalGcPressure; }
    public Duration getTransactionLongRunningThreshold() { return transactionLongRunningThreshold; }
    public void setTransactionLongRunningThreshold(Duration transactionLongRunningThreshold) { this.transactionLongRunningThreshold = normalizeDuration(transactionLongRunningThreshold, Duration.ofSeconds(8)); }
    public Duration getTransactionCriticalDurationThreshold() { return transactionCriticalDurationThreshold; }
    public void setTransactionCriticalDurationThreshold(Duration transactionCriticalDurationThreshold) { this.transactionCriticalDurationThreshold = normalizeDuration(transactionCriticalDurationThreshold, Duration.ofSeconds(20)); }
    public int getTransactionActiveThreshold() { return transactionActiveThreshold; }
    public void setTransactionActiveThreshold(int transactionActiveThreshold) { this.transactionActiveThreshold = Math.max(1, transactionActiveThreshold); }
    public int getTransactionBudgetViolationThreshold() { return transactionBudgetViolationThreshold; }
    public void setTransactionBudgetViolationThreshold(int transactionBudgetViolationThreshold) { this.transactionBudgetViolationThreshold = Math.max(1, transactionBudgetViolationThreshold); }
    public int getCriticalTransactionBudgetViolationThreshold() { return criticalTransactionBudgetViolationThreshold; }
    public void setCriticalTransactionBudgetViolationThreshold(int criticalTransactionBudgetViolationThreshold) { this.criticalTransactionBudgetViolationThreshold = Math.max(1, criticalTransactionBudgetViolationThreshold); }
    public boolean isFailReadyOnCriticalTransactionPressure() { return failReadyOnCriticalTransactionPressure; }
    public void setFailReadyOnCriticalTransactionPressure(boolean failReadyOnCriticalTransactionPressure) { this.failReadyOnCriticalTransactionPressure = failReadyOnCriticalTransactionPressure; }
    public boolean isFailReadyOnCriticalTransactionBudgetViolation() { return failReadyOnCriticalTransactionBudgetViolation; }
    public void setFailReadyOnCriticalTransactionBudgetViolation(boolean failReadyOnCriticalTransactionBudgetViolation) { this.failReadyOnCriticalTransactionBudgetViolation = failReadyOnCriticalTransactionBudgetViolation; }
    public List<String> getLiveNamespaces() { return liveNamespaces; }
    public long getLiveTotalSubscribersThreshold() { return liveTotalSubscribersThreshold; }
    public void setLiveTotalSubscribersThreshold(long liveTotalSubscribersThreshold) { this.liveTotalSubscribersThreshold = Math.max(1L, liveTotalSubscribersThreshold); }
    public long getLiveActiveTopicsThreshold() { return liveActiveTopicsThreshold; }
    public void setLiveActiveTopicsThreshold(long liveActiveTopicsThreshold) { this.liveActiveTopicsThreshold = Math.max(1L, liveActiveTopicsThreshold); }
    public long getLiveSubscriberRisingFastDelta() { return liveSubscriberRisingFastDelta; }
    public void setLiveSubscriberRisingFastDelta(long liveSubscriberRisingFastDelta) { this.liveSubscriberRisingFastDelta = Math.max(1L, liveSubscriberRisingFastDelta); }
    public Duration getLiveTrendWindow() { return liveTrendWindow; }
    public void setLiveTrendWindow(Duration liveTrendWindow) { this.liveTrendWindow = normalizeDuration(liveTrendWindow, Duration.ofSeconds(45)); }
    public Duration getLiveSustainedWindow() { return liveSustainedWindow; }
    public void setLiveSustainedWindow(Duration liveSustainedWindow) { this.liveSustainedWindow = normalizeDuration(liveSustainedWindow, Duration.ofMinutes(2)); }
    public boolean isFailReadyOnCriticalLiveSurge() { return failReadyOnCriticalLiveSurge; }
    public void setFailReadyOnCriticalLiveSurge(boolean failReadyOnCriticalLiveSurge) { this.failReadyOnCriticalLiveSurge = failReadyOnCriticalLiveSurge; }
    public double getKafkaBufferAvailableRatioThreshold() { return kafkaBufferAvailableRatioThreshold; }
    public void setKafkaBufferAvailableRatioThreshold(double kafkaBufferAvailableRatioThreshold) { this.kafkaBufferAvailableRatioThreshold = clampRatio(kafkaBufferAvailableRatioThreshold, 0.20d); }
    public double getKafkaRequestsInFlightThreshold() { return kafkaRequestsInFlightThreshold; }
    public void setKafkaRequestsInFlightThreshold(double kafkaRequestsInFlightThreshold) { this.kafkaRequestsInFlightThreshold = Math.max(1.0d, kafkaRequestsInFlightThreshold); }
    public double getKafkaRecordQueueTimeAvgThresholdMillis() { return kafkaRecordQueueTimeAvgThresholdMillis; }
    public void setKafkaRecordQueueTimeAvgThresholdMillis(double kafkaRecordQueueTimeAvgThresholdMillis) { this.kafkaRecordQueueTimeAvgThresholdMillis = Math.max(1.0d, kafkaRecordQueueTimeAvgThresholdMillis); }
    public double getKafkaRecordErrorRateThreshold() { return kafkaRecordErrorRateThreshold; }
    public void setKafkaRecordErrorRateThreshold(double kafkaRecordErrorRateThreshold) { this.kafkaRecordErrorRateThreshold = clampRatio(kafkaRecordErrorRateThreshold, 0.02d); }
    public boolean isFailReadyOnCriticalKafkaPressure() { return failReadyOnCriticalKafkaPressure; }
    public void setFailReadyOnCriticalKafkaPressure(boolean failReadyOnCriticalKafkaPressure) { this.failReadyOnCriticalKafkaPressure = failReadyOnCriticalKafkaPressure; }

    private static Duration normalizeDuration(Duration value, Duration fallback) {
        if (value == null || value.isNegative() || value.isZero()) {
            return fallback;
        }
        return value;
    }

    private static int clampScore(int value, int fallback) {
        if (value < 1 || value > 100) {
            return fallback;
        }
        return value;
    }

    private static double clampRatio(double value, double fallback) {
        if (Double.isNaN(value) || value <= 0.0d || value >= 1.0d) {
            return fallback;
        }
        return value;
    }
}
