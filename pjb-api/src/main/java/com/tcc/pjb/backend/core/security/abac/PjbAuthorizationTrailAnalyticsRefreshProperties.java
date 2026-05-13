package com.tcc.pjb.backend.core.security.abac;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.authz.analytics.refresh")
public class PjbAuthorizationTrailAnalyticsRefreshProperties {

    private boolean enabled = true;
    private long fixedRateMs = 5000L;
    private long processingTimeoutMs = 300000L;
    private long cleanupFixedRateMs = 3600000L;
    private int batchSize = 64;
    private int maxBackfillBuckets = 4096;
    private int completionRetentionDays = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedRateMs() {
        return fixedRateMs;
    }

    public void setFixedRateMs(long fixedRateMs) {
        this.fixedRateMs = fixedRateMs;
    }

    public long getProcessingTimeoutMs() {
        return processingTimeoutMs;
    }

    public void setProcessingTimeoutMs(long processingTimeoutMs) {
        this.processingTimeoutMs = processingTimeoutMs;
    }

    public long getCleanupFixedRateMs() {
        return cleanupFixedRateMs;
    }

    public void setCleanupFixedRateMs(long cleanupFixedRateMs) {
        this.cleanupFixedRateMs = cleanupFixedRateMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxBackfillBuckets() {
        return maxBackfillBuckets;
    }

    public void setMaxBackfillBuckets(int maxBackfillBuckets) {
        this.maxBackfillBuckets = maxBackfillBuckets;
    }

    public int getCompletionRetentionDays() {
        return completionRetentionDays;
    }

    public void setCompletionRetentionDays(int completionRetentionDays) {
        this.completionRetentionDays = completionRetentionDays;
    }
}
