package com.tcc.pjb.backend.core.governance.idempotency.cleanup;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.governance.idempotency.cleanup")
public class IdempotencyCleanupProperties {

    private boolean enabled = true;

    
    private Duration ttl = Duration.ofDays(30);

    
    private String cron = "0 17 3 * * *";

    
    private int batchSize = 10_000;

    
    private int maxBatchesPerRun = 50;

    
    private long advisoryLockKey = 892_331_771_210L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
            this.ttl = ttl;
        }
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        if (cron != null && !cron.isBlank()) {
            this.cron = cron.trim();
        }
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        if (batchSize > 0) this.batchSize = batchSize;
    }

    public int getMaxBatchesPerRun() {
        return maxBatchesPerRun;
    }

    public void setMaxBatchesPerRun(int maxBatchesPerRun) {
        if (maxBatchesPerRun > 0) this.maxBatchesPerRun = maxBatchesPerRun;
    }

    public long getAdvisoryLockKey() {
        return advisoryLockKey;
    }

    public void setAdvisoryLockKey(long advisoryLockKey) {
        this.advisoryLockKey = advisoryLockKey;
    }
}
