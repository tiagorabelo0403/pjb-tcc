package com.tcc.pjb.backend.service.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pjb.outbox.ingress")
public class OutboxIngressProperties {

    private boolean enabled = true;
    private int capacity = 4096;
    private int flushBatchSize = 128;
    private Duration flushInterval = Duration.ofMillis(100);
    private Duration offerTimeout = Duration.ofMillis(25);
    private boolean fallbackToDirectPersist = true;
    private boolean failFastWhenSaturated = false;
    private int historyLimit = 1024;
    private int maxPayloadBytes = 524288;
    private int maxHeadersBytes = 32768;
    private int maxMetadataEntries = 64;
    private int maxStringLength = 4096;
    private int flushBurstLimit = 8;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getFlushBatchSize() {
        return flushBatchSize;
    }

    public void setFlushBatchSize(int flushBatchSize) {
        this.flushBatchSize = flushBatchSize;
    }

    public Duration getFlushInterval() {
        return flushInterval;
    }

    public void setFlushInterval(Duration flushInterval) {
        this.flushInterval = flushInterval;
    }

    public Duration getOfferTimeout() {
        return offerTimeout;
    }

    public void setOfferTimeout(Duration offerTimeout) {
        this.offerTimeout = offerTimeout;
    }

    public boolean isFallbackToDirectPersist() {
        return fallbackToDirectPersist;
    }

    public void setFallbackToDirectPersist(boolean fallbackToDirectPersist) {
        this.fallbackToDirectPersist = fallbackToDirectPersist;
    }

    public boolean isFailFastWhenSaturated() {
        return failFastWhenSaturated;
    }

    public void setFailFastWhenSaturated(boolean failFastWhenSaturated) {
        this.failFastWhenSaturated = failFastWhenSaturated;
    }

    public int getHistoryLimit() {
        return historyLimit;
    }

    public void setHistoryLimit(int historyLimit) {
        this.historyLimit = historyLimit;
    }

    public int getMaxPayloadBytes() {
        return maxPayloadBytes;
    }

    public void setMaxPayloadBytes(int maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public int getMaxHeadersBytes() {
        return maxHeadersBytes;
    }

    public void setMaxHeadersBytes(int maxHeadersBytes) {
        this.maxHeadersBytes = maxHeadersBytes;
    }

    public int getMaxMetadataEntries() {
        return maxMetadataEntries;
    }

    public void setMaxMetadataEntries(int maxMetadataEntries) {
        this.maxMetadataEntries = maxMetadataEntries;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public void setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
    }

    public int getFlushBurstLimit() {
        return flushBurstLimit;
    }

    public void setFlushBurstLimit(int flushBurstLimit) {
        this.flushBurstLimit = flushBurstLimit;
    }
}
