package com.tcc.pjb.backend.core.observability.systemhealth;

import com.tcc.pjb.backend.configs.security.hardening.PjbOperationalCrisisProperties;
import com.tcc.pjb.backend.configs.security.hardening.PjbOperationalCrisisProperties.CrisisMode;
import com.tcc.pjb.backend.configs.security.hardening.PjbOperationalCrisisProperties.LaneDirective;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbOperationalCrisisService {

    private final PjbOperationalCrisisProperties properties;

    public PjbOperationalCrisisService(PjbOperationalCrisisProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public boolean isActive() {
        return properties.isActive();
    }

    public CrisisMode mode() {
        return properties.getMode();
    }

    public boolean emitDebugHeaders() {
        return properties.isEmitDebugHeaders();
    }

    public CrisisDecision evaluate(String uri, String laneName, int baseLaneLimit, Duration baseTimeout, int baseStatus, String baseCode) {
        if (!properties.isActive()) {
            return CrisisDecision.inactive(baseLaneLimit, baseTimeout, baseStatus, baseCode);
        }
        if (matches(properties.getBlockedPrefixes(), uri)) {
            return CrisisDecision.blocked(
                    properties.getRejectionStatus(),
                    properties.getRejectionCode(),
                    properties.getRejectionDetail(),
                    baseLaneLimit,
                    baseTimeout,
                    baseStatus,
                    baseCode,
                    properties.getMode().externalName()
            );
        }
        LaneDirective directive = findDirective(laneName);
        if (directive == null) {
            return CrisisDecision.active(baseLaneLimit, baseTimeout, baseStatus, baseCode, properties.getMode().externalName());
        }
        int effectiveLimit = directive.getMaxInFlightOverride() != null
                ? directive.getMaxInFlightOverride()
                : Math.max(1, (int) Math.ceil(baseLaneLimit * safeMultiplier(directive.getMaxInFlightMultiplier())));
        Duration effectiveTimeout = directive.getAcquireTimeoutOverride() != null ? directive.getAcquireTimeoutOverride() : baseTimeout;
        int effectiveStatus = directive.getRejectionStatus() != null ? directive.getRejectionStatus() : baseStatus;
        String effectiveCode = directive.getRejectionCode() == null || directive.getRejectionCode().isBlank() ? baseCode : directive.getRejectionCode().trim();
        if (directive.isRejectAll()) {
            return CrisisDecision.blocked(
                    effectiveStatus,
                    effectiveCode,
                    properties.getRejectionDetail(),
                    effectiveLimit,
                    effectiveTimeout,
                    effectiveStatus,
                    effectiveCode,
                    properties.getMode().externalName()
            );
        }
        return CrisisDecision.active(effectiveLimit, effectiveTimeout, effectiveStatus, effectiveCode, properties.getMode().externalName());
    }

    public boolean isExempt(String uri) {
        return matches(properties.getExemptPrefixes(), uri);
    }

    public Map<String, Object> exportState() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", properties.isEnabled());
        out.put("mode", properties.getMode().externalName());
        out.put("active", properties.isActive());
        out.put("blockedPrefixes", List.copyOf(properties.getBlockedPrefixes()));
        out.put("laneDirectives", properties.getLaneDirectives().stream()
                .filter(Objects::nonNull)
                .map(directive -> Map.<String, Object>of(
                        "name", defaultString(directive.getName(), ""),
                        "maxInFlightOverride", directive.getMaxInFlightOverride() == null ? -1 : directive.getMaxInFlightOverride(),
                        "maxInFlightMultiplier", safeMultiplier(directive.getMaxInFlightMultiplier()),
                        "rejectAll", directive.isRejectAll(),
                        "rejectionStatus", directive.getRejectionStatus() == null ? properties.getRejectionStatus() : directive.getRejectionStatus(),
                        "rejectionCode", defaultString(directive.getRejectionCode(), properties.getRejectionCode())
                ))
                .toList());
        return out;
    }

    private LaneDirective findDirective(String laneName) {
        if (laneName == null || laneName.isBlank()) {
            return null;
        }
        for (LaneDirective directive : properties.getLaneDirectives()) {
            if (directive == null || directive.getName() == null || directive.getName().isBlank()) {
                continue;
            }
            if (directive.getName().trim().equalsIgnoreCase(laneName.trim())) {
                return directive;
            }
        }
        return null;
    }

    private static boolean matches(List<String> prefixes, String uri) {
        if (uri == null || uri.isBlank() || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isBlank() && uri.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private static double safeMultiplier(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return 1.0d;
        }
        return Math.max(0.05d, value);
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record CrisisDecision(boolean active,
                                 boolean blocked,
                                 int laneLimit,
                                 Duration laneAcquireTimeout,
                                 int rejectionStatus,
                                 String rejectionCode,
                                 String detail,
                                 String mode) {

        static CrisisDecision inactive(int laneLimit, Duration laneAcquireTimeout, int rejectionStatus, String rejectionCode) {
            return new CrisisDecision(false, false, laneLimit, laneAcquireTimeout, rejectionStatus, rejectionCode, "", CrisisMode.NORMAL.externalName());
        }

        static CrisisDecision active(int laneLimit, Duration laneAcquireTimeout, int rejectionStatus, String rejectionCode, String mode) {
            return new CrisisDecision(true, false, laneLimit, laneAcquireTimeout, rejectionStatus, rejectionCode, "", mode);
        }

        static CrisisDecision blocked(int rejectionStatus,
                                      String rejectionCode,
                                      String detail,
                                      int laneLimit,
                                      Duration laneAcquireTimeout,
                                      int fallbackStatus,
                                      String fallbackCode,
                                      String mode) {
            int safeStatus = rejectionStatus < 400 ? fallbackStatus : rejectionStatus;
            String safeCode = rejectionCode == null || rejectionCode.isBlank() ? fallbackCode : rejectionCode.trim();
            String safeDetail = detail == null || detail.isBlank()
                    ? "A plataforma entrou em contenção operacional temporária para preservar as trilhas críticas."
                    : detail.trim();
            return new CrisisDecision(true, true, Math.max(1, laneLimit), laneAcquireTimeout, safeStatus, safeCode, safeDetail, mode);
        }
    }
}
