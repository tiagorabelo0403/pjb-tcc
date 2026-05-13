package com.tcc.pjb.backend.configs.security.hardening;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.api.crisis-control")
public class PjbOperationalCrisisProperties {

    private boolean enabled = false;
    private CrisisMode mode = CrisisMode.NORMAL;
    private int rejectionStatus = 503;
    private boolean emitDebugHeaders = false;
    private String rejectionCode = "CRISIS_CONTAINMENT";
    private String rejectionDetail = "A plataforma entrou em contenção operacional temporária para preservar as trilhas críticas.";
    private final List<String> exemptPrefixes = new ArrayList<>(List.of("/actuator/health", "/actuator/health/", "/livez", "/readyz", "/startupz"));
    private final List<String> blockedPrefixes = new ArrayList<>();
    private final List<LaneDirective> laneDirectives = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CrisisMode getMode() {
        return mode;
    }

    public void setMode(CrisisMode mode) {
        this.mode = mode == null ? CrisisMode.NORMAL : mode;
    }

    public int getRejectionStatus() {
        return rejectionStatus;
    }

    public void setRejectionStatus(int rejectionStatus) {
        this.rejectionStatus = rejectionStatus < 400 ? 503 : rejectionStatus;
    }

    public boolean isEmitDebugHeaders() {
        return emitDebugHeaders;
    }

    public void setEmitDebugHeaders(boolean emitDebugHeaders) {
        this.emitDebugHeaders = emitDebugHeaders;
    }

    public String getRejectionCode() {
        return rejectionCode;
    }

    public void setRejectionCode(String rejectionCode) {
        this.rejectionCode = normalizeCode(rejectionCode, "CRISIS_CONTAINMENT");
    }

    public String getRejectionDetail() {
        return rejectionDetail;
    }

    public void setRejectionDetail(String rejectionDetail) {
        this.rejectionDetail = rejectionDetail == null || rejectionDetail.isBlank()
                ? "A plataforma entrou em contenção operacional temporária para preservar as trilhas críticas."
                : rejectionDetail.trim();
    }

    public List<String> getExemptPrefixes() {
        return exemptPrefixes;
    }

    public List<String> getBlockedPrefixes() {
        return blockedPrefixes;
    }

    public List<LaneDirective> getLaneDirectives() {
        return laneDirectives;
    }

    public boolean isActive() {
        return enabled && mode != CrisisMode.NORMAL;
    }

    public enum CrisisMode {
        NORMAL,
        ELEVATED,
        CONTAINMENT,
        LOCKDOWN;

        public String externalName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static class LaneDirective {
        private String name;
        private Integer maxInFlightOverride;
        private Double maxInFlightMultiplier = 1.0d;
        private Duration acquireTimeoutOverride;
        private boolean rejectAll = false;
        private Integer rejectionStatus;
        private String rejectionCode = "CRISIS_LANE_CONTAINMENT";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name == null ? null : name.trim();
        }

        public Integer getMaxInFlightOverride() {
            return maxInFlightOverride;
        }

        public void setMaxInFlightOverride(Integer maxInFlightOverride) {
            this.maxInFlightOverride = maxInFlightOverride == null ? null : Math.max(1, maxInFlightOverride);
        }

        public Double getMaxInFlightMultiplier() {
            return maxInFlightMultiplier;
        }

        public void setMaxInFlightMultiplier(Double maxInFlightMultiplier) {
            if (maxInFlightMultiplier == null || maxInFlightMultiplier.isNaN() || maxInFlightMultiplier.isInfinite()) {
                this.maxInFlightMultiplier = 1.0d;
                return;
            }
            this.maxInFlightMultiplier = Math.max(0.05d, maxInFlightMultiplier);
        }

        public Duration getAcquireTimeoutOverride() {
            return acquireTimeoutOverride;
        }

        public void setAcquireTimeoutOverride(Duration acquireTimeoutOverride) {
            this.acquireTimeoutOverride = acquireTimeoutOverride == null || acquireTimeoutOverride.isNegative() ? null : acquireTimeoutOverride;
        }

        public boolean isRejectAll() {
            return rejectAll;
        }

        public void setRejectAll(boolean rejectAll) {
            this.rejectAll = rejectAll;
        }

        public Integer getRejectionStatus() {
            return rejectionStatus;
        }

        public void setRejectionStatus(Integer rejectionStatus) {
            this.rejectionStatus = rejectionStatus == null ? null : Math.max(400, rejectionStatus);
        }

        public String getRejectionCode() {
            return rejectionCode;
        }

        public void setRejectionCode(String rejectionCode) {
            this.rejectionCode = normalizeCode(rejectionCode, "CRISIS_LANE_CONTAINMENT");
        }
    }

    private static String normalizeCode(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
