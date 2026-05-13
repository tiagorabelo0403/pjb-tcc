package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.institutional.official-source.connectors")
public class InstitutionalOfficialSourceConnectorProperties {

    private boolean enabled = true;
    private long refreshHours = 12;
    private long probeTtlMinutes = 30;
    private long probeTimeoutMillis = 4000;
    private int probeBatchSize = 3;
    private final Map<String, SourceConfig> sources = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getRefreshHours() {
        return refreshHours;
    }

    public void setRefreshHours(long refreshHours) {
        this.refreshHours = refreshHours;
    }

    public long getProbeTtlMinutes() {
        return probeTtlMinutes;
    }

    public void setProbeTtlMinutes(long probeTtlMinutes) {
        this.probeTtlMinutes = probeTtlMinutes;
    }

    public long getProbeTimeoutMillis() {
        return probeTimeoutMillis;
    }

    public void setProbeTimeoutMillis(long probeTimeoutMillis) {
        this.probeTimeoutMillis = probeTimeoutMillis;
    }

    public int getProbeBatchSize() {
        return probeBatchSize;
    }

    public void setProbeBatchSize(int probeBatchSize) {
        this.probeBatchSize = probeBatchSize;
    }

    public Map<String, SourceConfig> getSources() {
        return sources;
    }

    public static class SourceConfig {
        private boolean enabled = true;
        private boolean dryRun = true;
        private String baseUrl;
        private String referenceUrl;
        private String apiKey;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getReferenceUrl() {
            return referenceUrl;
        }

        public void setReferenceUrl(String referenceUrl) {
            this.referenceUrl = referenceUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
