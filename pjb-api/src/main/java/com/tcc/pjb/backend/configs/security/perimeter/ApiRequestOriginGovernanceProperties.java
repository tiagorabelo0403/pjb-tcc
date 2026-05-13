package com.tcc.pjb.backend.configs.security.perimeter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.security.perimeter.origin-governance")
public class ApiRequestOriginGovernanceProperties {

    private boolean enabled;
    private boolean allowRefererFallback = true;
    private boolean requireBodyHashOnSignedJsonRequests = true;
    private Duration maxTimestampSkew = Duration.ofMinutes(5);
    private final List<String> exemptPrefixes = new ArrayList<>(List.of(
            "/actuator/health",
            "/livez",
            "/readyz",
            "/startupz",
            "/internal/runtime/"
    ));
    private final List<String> governedPrefixes = new ArrayList<>(List.of(
            "/api/v1/auth/",
            "/api/v1/security/",
            "/api/ai/",
            "/api/v1/ia/",
            "/api/v1/peticionamento/",
            "/api/v1/processual/",
            "/api/v1/processos/",
            "/api/v1/institucional/",
            "/api/v1/advogado/",
            "/api/v1/oab/",
            "/api/marketplace/v1/"
    ));
    private final List<String> signedRequiredPrefixes = new ArrayList<>();
    private final List<String> trustedBrowserOrigins = new ArrayList<>();
    private final List<TrustedOrigin> trustedOrigins = new ArrayList<>();
    private final List<SelectiveSignedRule> selectiveSignedRules = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowRefererFallback() {
        return allowRefererFallback;
    }

    public void setAllowRefererFallback(boolean allowRefererFallback) {
        this.allowRefererFallback = allowRefererFallback;
    }

    public boolean isRequireBodyHashOnSignedJsonRequests() {
        return requireBodyHashOnSignedJsonRequests;
    }

    public void setRequireBodyHashOnSignedJsonRequests(boolean requireBodyHashOnSignedJsonRequests) {
        this.requireBodyHashOnSignedJsonRequests = requireBodyHashOnSignedJsonRequests;
    }

    public Duration getMaxTimestampSkew() {
        return maxTimestampSkew;
    }

    public void setMaxTimestampSkew(Duration maxTimestampSkew) {
        this.maxTimestampSkew = maxTimestampSkew == null || maxTimestampSkew.isNegative() || maxTimestampSkew.isZero()
                ? Duration.ofMinutes(5)
                : maxTimestampSkew;
    }

    public List<String> getExemptPrefixes() {
        return exemptPrefixes;
    }

    public List<String> getGovernedPrefixes() {
        return governedPrefixes;
    }

    public List<String> getSignedRequiredPrefixes() {
        return signedRequiredPrefixes;
    }

    public List<String> getTrustedBrowserOrigins() {
        return trustedBrowserOrigins;
    }

    public List<TrustedOrigin> getTrustedOrigins() {
        return trustedOrigins;
    }

    public List<SelectiveSignedRule> getSelectiveSignedRules() {
        return selectiveSignedRules;
    }

    public static class SelectiveSignedRule {
        private String name;
        private final List<String> paths = new ArrayList<>();
        private final List<String> capabilityValues = new ArrayList<>();
        private final List<String> capabilityJsonPointers = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = normalize(name);
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> values) {
            replace(paths, values, false);
        }

        public List<String> getCapabilityValues() {
            return capabilityValues;
        }

        public void setCapabilityValues(List<String> values) {
            replace(capabilityValues, values, true);
        }

        public List<String> getCapabilityJsonPointers() {
            return capabilityJsonPointers;
        }

        public void setCapabilityJsonPointers(List<String> values) {
            replace(capabilityJsonPointers, values, false);
        }
    }

    public static class TrustedOrigin {
        private boolean active;
        private String id;
        private String secret;
        private final List<String> allowedCidrs = new ArrayList<>();
        private final List<String> allowedPathPrefixes = new ArrayList<>();
        private final List<String> allowedMethods = new ArrayList<>();
        private final List<String> allowedOrigins = new ArrayList<>();

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = normalize(id);
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret == null ? null : secret.trim();
        }

        public List<String> getAllowedCidrs() {
            return allowedCidrs;
        }

        public void setAllowedCidrs(List<String> values) {
            replace(allowedCidrs, values, false);
        }

        public List<String> getAllowedPathPrefixes() {
            return allowedPathPrefixes;
        }

        public void setAllowedPathPrefixes(List<String> values) {
            replace(allowedPathPrefixes, values, false);
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> values) {
            replace(allowedMethods, values, true);
        }

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> values) {
            replace(allowedOrigins, values, false);
        }

    }

    private static void replace(List<String> target, List<String> values, boolean upperCase) {
        target.clear();
        if (values == null || values.isEmpty()) {
            return;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized == null) {
                continue;
            }
            unique.add(upperCase ? normalized.toUpperCase(Locale.ROOT) : normalized);
        }
        target.addAll(unique);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
