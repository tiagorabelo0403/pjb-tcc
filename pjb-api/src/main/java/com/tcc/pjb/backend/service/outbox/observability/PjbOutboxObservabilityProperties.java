package com.tcc.pjb.backend.service.outbox.observability;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.outbox.observability")
public class PjbOutboxObservabilityProperties {

    private boolean enabled = true;
    private Duration refreshInterval = Duration.ofSeconds(30);
    private long failedThreshold = 100;
    private Duration inflightStaleAfter = Duration.ofMinutes(5);
    private Duration pendingLagAfter = Duration.ofMinutes(10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public long getFailedThreshold() {
        return failedThreshold;
    }

    public void setFailedThreshold(long failedThreshold) {
        this.failedThreshold = failedThreshold;
    }

    public Duration getInflightStaleAfter() {
        return inflightStaleAfter;
    }

    public void setInflightStaleAfter(Duration inflightStaleAfter) {
        this.inflightStaleAfter = inflightStaleAfter;
    }

    public Duration getPendingLagAfter() {
        return pendingLagAfter;
    }

    public void setPendingLagAfter(Duration pendingLagAfter) {
        this.pendingLagAfter = pendingLagAfter;
    }
}
