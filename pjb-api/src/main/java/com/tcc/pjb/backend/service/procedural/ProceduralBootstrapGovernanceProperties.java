package com.tcc.pjb.backend.service.procedural;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.procedural.bootstrap")
public class ProceduralBootstrapGovernanceProperties {

    private boolean enabled = true;
    private boolean failFast = false;
    private boolean validateLegacyBoundary = true;
    private boolean strictConnectorRegistry = false;
    private int maxViolations = 50;
    private List<String> failFastProfiles = new ArrayList<>(List.of("prod"));
    private List<String> strictConnectorRegistryProfiles = new ArrayList<>(List.of("prod", "strict"));
    private List<String> sourceRoots = new ArrayList<>(List.of("src/main/java"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public boolean isValidateLegacyBoundary() {
        return validateLegacyBoundary;
    }

    public void setValidateLegacyBoundary(boolean validateLegacyBoundary) {
        this.validateLegacyBoundary = validateLegacyBoundary;
    }

    public boolean isStrictConnectorRegistry() {
        return strictConnectorRegistry;
    }

    public void setStrictConnectorRegistry(boolean strictConnectorRegistry) {
        this.strictConnectorRegistry = strictConnectorRegistry;
    }

    public int getMaxViolations() {
        return maxViolations;
    }

    public void setMaxViolations(int maxViolations) {
        this.maxViolations = Math.max(1, maxViolations);
    }

    public List<String> getFailFastProfiles() {
        return List.copyOf(failFastProfiles);
    }

    public void setFailFastProfiles(List<String> failFastProfiles) {
        this.failFastProfiles = sanitizeList(failFastProfiles, List.of("prod"));
    }

    public List<String> getStrictConnectorRegistryProfiles() {
        return List.copyOf(strictConnectorRegistryProfiles);
    }

    public void setStrictConnectorRegistryProfiles(List<String> strictConnectorRegistryProfiles) {
        this.strictConnectorRegistryProfiles = sanitizeList(strictConnectorRegistryProfiles, List.of("prod", "strict"));
    }

    public List<String> getSourceRoots() {
        return List.copyOf(sourceRoots);
    }

    public void setSourceRoots(List<String> sourceRoots) {
        this.sourceRoots = sanitizeList(sourceRoots, List.of("src/main/java"));
    }

    private static List<String> sanitizeList(List<String> raw, List<String> fallback) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>(fallback);
        }
        ArrayList<String> out = new ArrayList<>();
        for (String value : raw) {
            for (String normalized : normalizeConfiguredListEntry(value)) {
                if (!normalized.isBlank()) {
                    out.add(normalized);
                }
            }
        }
        return out.isEmpty() ? new ArrayList<>(fallback) : out;
    }

    private static List<String> normalizeConfiguredListEntry(String value) {
        if (value == null) {
            return List.of();
        }
        String normalized = stripBoundaryQuotes(value.trim());
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            String body = normalized.substring(1, normalized.length() - 1).trim();
            if (body.isBlank()) {
                return List.of();
            }
            ArrayList<String> out = new ArrayList<>();
            for (String item : body.split(",")) {
                String candidate = stripBoundaryQuotes(item.trim());
                if (!candidate.isBlank()) {
                    out.add(candidate);
                }
            }
            return out;
        }
        return List.of(normalized);
    }

    private static String stripBoundaryQuotes(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.length() >= 2
                && ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'")))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }
}
