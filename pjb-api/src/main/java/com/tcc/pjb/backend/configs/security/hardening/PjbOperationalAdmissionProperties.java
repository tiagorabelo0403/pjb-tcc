package com.tcc.pjb.backend.configs.security.hardening;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.runtime.admission")
public class PjbOperationalAdmissionProperties {

    private boolean enabled = true;
    private Duration decisionTtl = Duration.ofMillis(150);
    private int softPressureScoreThreshold = 55;
    private int hardPressureScoreThreshold = 72;
    private boolean rejectDuringWarmup = true;
    private boolean rejectDuringDrain = true;
    private boolean rejectWriteOnCriticalDatasourceRunaway = true;
    private boolean rejectExpensiveOnSchedulerTrendingUp = true;
    private boolean rejectExpensiveWhenPressureRising = true;
    private boolean rejectOnPressureSustained = true;
    private boolean rejectStreamOnMemoryPressure = true;
    private boolean rejectBulkOnPressureSustained = true;
    private boolean rejectExportOnPressureRising = true;
    private boolean rejectExpensiveOnCriticalMemoryRunaway = true;
    private boolean rejectExpensiveOnCriticalGcPressure = true;
    private boolean rejectEventStreamOnLiveSurge = true;
    private boolean rejectWriteOnKafkaBackpressure = true;
    private boolean rejectVeryLargeWriteOnPressure = true;
    private boolean rejectVeryLargeReadOnPressure = true;
    private long largeContentLengthBytes = 4L * 1024L * 1024L;
    private long veryLargeContentLengthBytes = 16L * 1024L * 1024L;
    private int largePageSizeThreshold = 500;
    private long largeEstimatedItemsThreshold = 10_000L;
    private int softRejectionStatus = 429;
    private int hardRejectionStatus = 503;
    private int retryAfterSeconds = 1;
    private int decisionCacheMaxEntries = 512;
    private boolean emitDebugHeaders = false;
    private boolean preserveHighPriorityReadOnSoftPressure = true;
    private boolean rejectLowPriorityEarlier = true;
    private boolean rejectLowPriorityOnReservedLaneOverload = true;
    private boolean rejectNormalPriorityOnReservedLaneCollapse = true;
    private boolean rejectLowPriorityOnReservedLaneBudgetBreach = true;
    private boolean rejectNormalPriorityOnPreferredLaneBudgetBreach = true;
    private int maxReservedLaneHotspotsBeforeReject = 0;
    private int maxReservedLaneHotspotsBeforeCollapse = 1;
    private double reservedLaneAverageUtilizationThreshold = 0.82d;
    private int reservedLaneHeadroomScoreThreshold = 28;
    private double preferredLaneAverageUtilizationThreshold = 0.86d;
    private int preferredLaneHeadroomScoreThreshold = 20;
    private final List<String> exemptPrefixes = new ArrayList<>(List.of(
            "/actuator/health",
            "/actuator/health/",
            "/livez",
            "/readyz",
            "/startupz",
            "/internal/runtime/"
    ));
    private final List<String> guardedPrefixes = new ArrayList<>(List.of(
            "/api/v1/peticionamento",
            "/api/v1/laiane",
            "/api/v1/processual",
            "/api/v1/work-items",
            "/api/v1/secretariat",
            "/api/v1/juiz",
            "/api/v1/oficial",
            "/api/v1/delegado",
            "/api/v1/cidadao"
    ));
    private final List<String> expensivePrefixes = new ArrayList<>(List.of(
            "/api/v1/peticionamento",
            "/api/v1/laiane",
            "/api/v1/work-items",
            "/api/v1/secretariat",
            "/api/v1/juiz",
            "/api/v1/oficial",
            "/api/v1/delegado"
    ));
    private final List<String> writeSensitivePrefixes = new ArrayList<>(List.of(
            "/api/v1/peticionamento",
            "/api/v1/processual",
            "/api/v1/secretariat",
            "/api/v1/juiz",
            "/api/v1/oficial",
            "/api/v1/delegado"
    ));
    private final List<String> criticalPrefixes = new ArrayList<>(List.of(
            "/api/v1/cidadao/painel",
            "/api/v1/processual/consulta",
            "/api/v1/processual/prazos",
            "/api/v1/juiz/painel",
            "/api/v1/oficial/painel"
    ));
    private final List<String> apiHighPriorityPrefixes = new ArrayList<>(List.of(
            "/api/v1/cidadao",
            "/api/v1/processual",
            "/api/v1/juiz/painel",
            "/api/v1/oficial/painel"
    ));
    private final List<String> workerHighPriorityPrefixes = new ArrayList<>(List.of(
            "/api/v1/work-items",
            "/api/v1/secretariat",
            "/api/v1/delegado"
    ));
    private final List<String> lowPriorityPrefixes = new ArrayList<>(List.of(
            "/export",
            "/relatorio",
            "/report",
            "/analytics",
            "/bulk",
            "/lote",
            "/massivo"
    ));
    private final List<String> apiReservedLanes = new ArrayList<>(List.of("external-io", "live", "io"));
    private final List<String> workerReservedLanes = new ArrayList<>(List.of("job", "io"));

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Duration getDecisionTtl() { return decisionTtl; }
    public void setDecisionTtl(Duration decisionTtl) { this.decisionTtl = normalizeDuration(decisionTtl, Duration.ofMillis(150)); }
    public int getSoftPressureScoreThreshold() { return softPressureScoreThreshold; }
    public void setSoftPressureScoreThreshold(int softPressureScoreThreshold) { this.softPressureScoreThreshold = clampScore(softPressureScoreThreshold, 55); }
    public int getHardPressureScoreThreshold() { return hardPressureScoreThreshold; }
    public void setHardPressureScoreThreshold(int hardPressureScoreThreshold) { this.hardPressureScoreThreshold = clampScore(hardPressureScoreThreshold, 72); }
    public boolean isRejectDuringWarmup() { return rejectDuringWarmup; }
    public void setRejectDuringWarmup(boolean rejectDuringWarmup) { this.rejectDuringWarmup = rejectDuringWarmup; }
    public boolean isRejectDuringDrain() { return rejectDuringDrain; }
    public void setRejectDuringDrain(boolean rejectDuringDrain) { this.rejectDuringDrain = rejectDuringDrain; }
    public boolean isRejectWriteOnCriticalDatasourceRunaway() { return rejectWriteOnCriticalDatasourceRunaway; }
    public void setRejectWriteOnCriticalDatasourceRunaway(boolean rejectWriteOnCriticalDatasourceRunaway) { this.rejectWriteOnCriticalDatasourceRunaway = rejectWriteOnCriticalDatasourceRunaway; }
    public boolean isRejectExpensiveOnSchedulerTrendingUp() { return rejectExpensiveOnSchedulerTrendingUp; }
    public void setRejectExpensiveOnSchedulerTrendingUp(boolean rejectExpensiveOnSchedulerTrendingUp) { this.rejectExpensiveOnSchedulerTrendingUp = rejectExpensiveOnSchedulerTrendingUp; }
    public boolean isRejectExpensiveWhenPressureRising() { return rejectExpensiveWhenPressureRising; }
    public void setRejectExpensiveWhenPressureRising(boolean rejectExpensiveWhenPressureRising) { this.rejectExpensiveWhenPressureRising = rejectExpensiveWhenPressureRising; }
    public boolean isRejectOnPressureSustained() { return rejectOnPressureSustained; }
    public void setRejectOnPressureSustained(boolean rejectOnPressureSustained) { this.rejectOnPressureSustained = rejectOnPressureSustained; }
    public boolean isRejectStreamOnMemoryPressure() { return rejectStreamOnMemoryPressure; }
    public void setRejectStreamOnMemoryPressure(boolean rejectStreamOnMemoryPressure) { this.rejectStreamOnMemoryPressure = rejectStreamOnMemoryPressure; }
    public boolean isRejectBulkOnPressureSustained() { return rejectBulkOnPressureSustained; }
    public void setRejectBulkOnPressureSustained(boolean rejectBulkOnPressureSustained) { this.rejectBulkOnPressureSustained = rejectBulkOnPressureSustained; }
    public boolean isRejectExportOnPressureRising() { return rejectExportOnPressureRising; }
    public void setRejectExportOnPressureRising(boolean rejectExportOnPressureRising) { this.rejectExportOnPressureRising = rejectExportOnPressureRising; }
    public boolean isRejectExpensiveOnCriticalMemoryRunaway() { return rejectExpensiveOnCriticalMemoryRunaway; }
    public void setRejectExpensiveOnCriticalMemoryRunaway(boolean rejectExpensiveOnCriticalMemoryRunaway) { this.rejectExpensiveOnCriticalMemoryRunaway = rejectExpensiveOnCriticalMemoryRunaway; }
    public boolean isRejectExpensiveOnCriticalGcPressure() { return rejectExpensiveOnCriticalGcPressure; }
    public void setRejectExpensiveOnCriticalGcPressure(boolean rejectExpensiveOnCriticalGcPressure) { this.rejectExpensiveOnCriticalGcPressure = rejectExpensiveOnCriticalGcPressure; }
    public boolean isRejectEventStreamOnLiveSurge() { return rejectEventStreamOnLiveSurge; }
    public void setRejectEventStreamOnLiveSurge(boolean rejectEventStreamOnLiveSurge) { this.rejectEventStreamOnLiveSurge = rejectEventStreamOnLiveSurge; }
    public boolean isRejectWriteOnKafkaBackpressure() { return rejectWriteOnKafkaBackpressure; }
    public void setRejectWriteOnKafkaBackpressure(boolean rejectWriteOnKafkaBackpressure) { this.rejectWriteOnKafkaBackpressure = rejectWriteOnKafkaBackpressure; }
    public boolean isRejectVeryLargeWriteOnPressure() { return rejectVeryLargeWriteOnPressure; }
    public void setRejectVeryLargeWriteOnPressure(boolean rejectVeryLargeWriteOnPressure) { this.rejectVeryLargeWriteOnPressure = rejectVeryLargeWriteOnPressure; }
    public boolean isRejectVeryLargeReadOnPressure() { return rejectVeryLargeReadOnPressure; }
    public void setRejectVeryLargeReadOnPressure(boolean rejectVeryLargeReadOnPressure) { this.rejectVeryLargeReadOnPressure = rejectVeryLargeReadOnPressure; }
    public long getLargeContentLengthBytes() { return largeContentLengthBytes; }
    public void setLargeContentLengthBytes(long largeContentLengthBytes) { this.largeContentLengthBytes = Math.max(32_768L, largeContentLengthBytes); }
    public long getVeryLargeContentLengthBytes() { return veryLargeContentLengthBytes; }
    public void setVeryLargeContentLengthBytes(long veryLargeContentLengthBytes) { this.veryLargeContentLengthBytes = Math.max(getLargeContentLengthBytes(), veryLargeContentLengthBytes); }
    public int getLargePageSizeThreshold() { return largePageSizeThreshold; }
    public void setLargePageSizeThreshold(int largePageSizeThreshold) { this.largePageSizeThreshold = Math.max(50, largePageSizeThreshold); }
    public long getLargeEstimatedItemsThreshold() { return largeEstimatedItemsThreshold; }
    public void setLargeEstimatedItemsThreshold(long largeEstimatedItemsThreshold) { this.largeEstimatedItemsThreshold = Math.max(200L, largeEstimatedItemsThreshold); }
    public int getSoftRejectionStatus() { return softRejectionStatus; }
    public void setSoftRejectionStatus(int softRejectionStatus) { this.softRejectionStatus = softRejectionStatus < 400 ? 429 : softRejectionStatus; }
    public int getHardRejectionStatus() { return hardRejectionStatus; }
    public void setHardRejectionStatus(int hardRejectionStatus) { this.hardRejectionStatus = hardRejectionStatus < 400 ? 503 : hardRejectionStatus; }
    public int getRetryAfterSeconds() { return retryAfterSeconds; }
    public void setRetryAfterSeconds(int retryAfterSeconds) { this.retryAfterSeconds = Math.max(0, retryAfterSeconds); }
    public int getDecisionCacheMaxEntries() { return decisionCacheMaxEntries; }
    public void setDecisionCacheMaxEntries(int decisionCacheMaxEntries) { this.decisionCacheMaxEntries = Math.max(32, decisionCacheMaxEntries); }
    public boolean isEmitDebugHeaders() { return emitDebugHeaders; }
    public void setEmitDebugHeaders(boolean emitDebugHeaders) { this.emitDebugHeaders = emitDebugHeaders; }
    public boolean isPreserveHighPriorityReadOnSoftPressure() { return preserveHighPriorityReadOnSoftPressure; }
    public void setPreserveHighPriorityReadOnSoftPressure(boolean preserveHighPriorityReadOnSoftPressure) { this.preserveHighPriorityReadOnSoftPressure = preserveHighPriorityReadOnSoftPressure; }
    public boolean isRejectLowPriorityEarlier() { return rejectLowPriorityEarlier; }
    public void setRejectLowPriorityEarlier(boolean rejectLowPriorityEarlier) { this.rejectLowPriorityEarlier = rejectLowPriorityEarlier; }
    public boolean isRejectLowPriorityOnReservedLaneOverload() { return rejectLowPriorityOnReservedLaneOverload; }
    public void setRejectLowPriorityOnReservedLaneOverload(boolean rejectLowPriorityOnReservedLaneOverload) { this.rejectLowPriorityOnReservedLaneOverload = rejectLowPriorityOnReservedLaneOverload; }
    public boolean isRejectNormalPriorityOnReservedLaneCollapse() { return rejectNormalPriorityOnReservedLaneCollapse; }
    public void setRejectNormalPriorityOnReservedLaneCollapse(boolean rejectNormalPriorityOnReservedLaneCollapse) { this.rejectNormalPriorityOnReservedLaneCollapse = rejectNormalPriorityOnReservedLaneCollapse; }
    public boolean isRejectLowPriorityOnReservedLaneBudgetBreach() { return rejectLowPriorityOnReservedLaneBudgetBreach; }
    public void setRejectLowPriorityOnReservedLaneBudgetBreach(boolean rejectLowPriorityOnReservedLaneBudgetBreach) { this.rejectLowPriorityOnReservedLaneBudgetBreach = rejectLowPriorityOnReservedLaneBudgetBreach; }
    public boolean isRejectNormalPriorityOnPreferredLaneBudgetBreach() { return rejectNormalPriorityOnPreferredLaneBudgetBreach; }
    public void setRejectNormalPriorityOnPreferredLaneBudgetBreach(boolean rejectNormalPriorityOnPreferredLaneBudgetBreach) { this.rejectNormalPriorityOnPreferredLaneBudgetBreach = rejectNormalPriorityOnPreferredLaneBudgetBreach; }
    public int getMaxReservedLaneHotspotsBeforeReject() { return maxReservedLaneHotspotsBeforeReject; }
    public void setMaxReservedLaneHotspotsBeforeReject(int maxReservedLaneHotspotsBeforeReject) { this.maxReservedLaneHotspotsBeforeReject = Math.max(0, maxReservedLaneHotspotsBeforeReject); }
    public int getMaxReservedLaneHotspotsBeforeCollapse() { return maxReservedLaneHotspotsBeforeCollapse; }
    public void setMaxReservedLaneHotspotsBeforeCollapse(int maxReservedLaneHotspotsBeforeCollapse) { this.maxReservedLaneHotspotsBeforeCollapse = Math.max(0, maxReservedLaneHotspotsBeforeCollapse); }
    public double getReservedLaneAverageUtilizationThreshold() { return reservedLaneAverageUtilizationThreshold; }
    public void setReservedLaneAverageUtilizationThreshold(double reservedLaneAverageUtilizationThreshold) { this.reservedLaneAverageUtilizationThreshold = clampRatio(reservedLaneAverageUtilizationThreshold, 0.82d); }
    public int getReservedLaneHeadroomScoreThreshold() { return reservedLaneHeadroomScoreThreshold; }
    public void setReservedLaneHeadroomScoreThreshold(int reservedLaneHeadroomScoreThreshold) { this.reservedLaneHeadroomScoreThreshold = clampPercentage(reservedLaneHeadroomScoreThreshold, 28); }
    public double getPreferredLaneAverageUtilizationThreshold() { return preferredLaneAverageUtilizationThreshold; }
    public void setPreferredLaneAverageUtilizationThreshold(double preferredLaneAverageUtilizationThreshold) { this.preferredLaneAverageUtilizationThreshold = clampRatio(preferredLaneAverageUtilizationThreshold, 0.86d); }
    public int getPreferredLaneHeadroomScoreThreshold() { return preferredLaneHeadroomScoreThreshold; }
    public void setPreferredLaneHeadroomScoreThreshold(int preferredLaneHeadroomScoreThreshold) { this.preferredLaneHeadroomScoreThreshold = clampPercentage(preferredLaneHeadroomScoreThreshold, 20); }
    public List<String> getExemptPrefixes() { return exemptPrefixes; }
    public List<String> getGuardedPrefixes() { return guardedPrefixes; }
    public List<String> getExpensivePrefixes() { return expensivePrefixes; }
    public List<String> getWriteSensitivePrefixes() { return writeSensitivePrefixes; }
    public List<String> getCriticalPrefixes() { return criticalPrefixes; }
    public List<String> getApiHighPriorityPrefixes() { return apiHighPriorityPrefixes; }
    public List<String> getWorkerHighPriorityPrefixes() { return workerHighPriorityPrefixes; }
    public List<String> getLowPriorityPrefixes() { return lowPriorityPrefixes; }
    public List<String> getApiReservedLanes() { return apiReservedLanes; }
    public List<String> getWorkerReservedLanes() { return workerReservedLanes; }

    private static int clampScore(int value, int fallback) {
        if (value < 1 || value > 100) {
            return fallback;
        }
        return value;
    }

    private static Duration normalizeDuration(Duration value, Duration fallback) {
        if (value == null || value.isNegative() || value.isZero()) {
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

    private static int clampPercentage(int value, int fallback) {
        if (value < 0 || value > 100) {
            return fallback;
        }
        return value;
    }
}
