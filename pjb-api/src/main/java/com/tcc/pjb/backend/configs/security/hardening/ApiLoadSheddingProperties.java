package com.tcc.pjb.backend.configs.security.hardening;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.api.load-shedding")
public class ApiLoadSheddingProperties {

    private boolean enabled = true;
    private int globalMaxInFlight = 640;
    private Duration globalAcquireTimeout = Duration.ofMillis(15);
    private boolean emitDebugHeaders = true;
    private final List<String> exemptPrefixes = new ArrayList<>(List.of("/actuator/health", "/actuator/health/", "/livez", "/readyz", "/startupz"));
    private final List<Rule> rules = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getGlobalMaxInFlight() { return globalMaxInFlight; }
    public void setGlobalMaxInFlight(int globalMaxInFlight) { this.globalMaxInFlight = Math.max(32, globalMaxInFlight); }
    public Duration getGlobalAcquireTimeout() { return globalAcquireTimeout; }
    public void setGlobalAcquireTimeout(Duration globalAcquireTimeout) { this.globalAcquireTimeout = normalizeTimeout(globalAcquireTimeout, Duration.ofMillis(15)); }
    public boolean isEmitDebugHeaders() { return emitDebugHeaders; }
    public void setEmitDebugHeaders(boolean emitDebugHeaders) { this.emitDebugHeaders = emitDebugHeaders; }
    public List<String> getExemptPrefixes() { return exemptPrefixes; }
    public List<Rule> getRules() { return rules; }

    public static class Rule {
        private String name;
        private List<String> prefixes = new ArrayList<>();
        private int maxInFlight = 96;
        private Duration acquireTimeout = Duration.ofMillis(25);
        private int rejectionStatus = 503;
        private String rejectionCode = "LOAD_SHED";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getPrefixes() { return prefixes; }
        public void setPrefixes(List<String> prefixes) { this.prefixes = prefixes == null ? new ArrayList<>() : new ArrayList<>(prefixes); }
        public int getMaxInFlight() { return maxInFlight; }
        public void setMaxInFlight(int maxInFlight) { this.maxInFlight = Math.max(1, maxInFlight); }
        public Duration getAcquireTimeout() { return acquireTimeout; }
        public void setAcquireTimeout(Duration acquireTimeout) { this.acquireTimeout = normalizeTimeout(acquireTimeout, Duration.ofMillis(25)); }
        public int getRejectionStatus() { return rejectionStatus; }
        public void setRejectionStatus(int rejectionStatus) { this.rejectionStatus = rejectionStatus < 400 ? 503 : rejectionStatus; }
        public String getRejectionCode() { return rejectionCode; }
        public void setRejectionCode(String rejectionCode) { this.rejectionCode = rejectionCode == null || rejectionCode.isBlank() ? "LOAD_SHED" : rejectionCode.trim(); }
    }

    private static Duration normalizeTimeout(Duration value, Duration fallback) {
        if (value == null || value.isNegative()) {
            return fallback;
        }
        return value;
    }
}
