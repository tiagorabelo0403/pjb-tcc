package com.tcc.pjb.backend.configs.security.hardening;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.api.db-pressure-shield")
public class ApiDatabasePressureShieldProperties {

    private boolean enabled = true;
    private double writeActiveRatioThreshold = 0.92d;
    private double readActiveRatioThreshold = 0.95d;
    private int writeThreadsAwaitingThreshold = 24;
    private int readThreadsAwaitingThreshold = 48;
    private Duration minDecisionTtl = Duration.ofMillis(100);
    private int rejectionStatus = 503;
    private String rejectionCode = "DB_PRESSURE_SHIELD";
    private boolean emitDebugHeaders;
    private final List<String> exemptPrefixes = new ArrayList<>(List.of("/actuator/health", "/actuator/health/", "/livez", "/readyz", "/startupz"));
    private final List<String> guardedPrefixes = new ArrayList<>(List.of("/api/v1/peticionamento", "/api/v1/laiane", "/api/v1/processual", "/api/v1/work-items", "/api/v1/secretariat", "/api/v1/juiz"));

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public double getWriteActiveRatioThreshold() { return writeActiveRatioThreshold; }
    public void setWriteActiveRatioThreshold(double writeActiveRatioThreshold) { this.writeActiveRatioThreshold = normalizeRatio(writeActiveRatioThreshold, 0.92d); }
    public double getReadActiveRatioThreshold() { return readActiveRatioThreshold; }
    public void setReadActiveRatioThreshold(double readActiveRatioThreshold) { this.readActiveRatioThreshold = normalizeRatio(readActiveRatioThreshold, 0.95d); }
    public int getWriteThreadsAwaitingThreshold() { return writeThreadsAwaitingThreshold; }
    public void setWriteThreadsAwaitingThreshold(int writeThreadsAwaitingThreshold) { this.writeThreadsAwaitingThreshold = Math.max(1, writeThreadsAwaitingThreshold); }
    public int getReadThreadsAwaitingThreshold() { return readThreadsAwaitingThreshold; }
    public void setReadThreadsAwaitingThreshold(int readThreadsAwaitingThreshold) { this.readThreadsAwaitingThreshold = Math.max(1, readThreadsAwaitingThreshold); }
    public Duration getMinDecisionTtl() { return minDecisionTtl; }
    public void setMinDecisionTtl(Duration minDecisionTtl) { this.minDecisionTtl = minDecisionTtl == null || minDecisionTtl.isNegative() ? Duration.ofMillis(100) : minDecisionTtl; }
    public int getRejectionStatus() { return rejectionStatus; }
    public void setRejectionStatus(int rejectionStatus) { this.rejectionStatus = rejectionStatus < 400 ? 503 : rejectionStatus; }
    public String getRejectionCode() { return rejectionCode; }
    public void setRejectionCode(String rejectionCode) { this.rejectionCode = rejectionCode == null || rejectionCode.isBlank() ? "DB_PRESSURE_SHIELD" : rejectionCode.trim(); }
    public boolean isEmitDebugHeaders() { return emitDebugHeaders; }
    public void setEmitDebugHeaders(boolean emitDebugHeaders) { this.emitDebugHeaders = emitDebugHeaders; }
    public List<String> getExemptPrefixes() { return exemptPrefixes; }
    public List<String> getGuardedPrefixes() { return guardedPrefixes; }

    private static double normalizeRatio(double value, double fallback) {
        if (Double.isNaN(value) || value <= 0d || value >= 1d) {
            return fallback;
        }
        return value;
    }
}
