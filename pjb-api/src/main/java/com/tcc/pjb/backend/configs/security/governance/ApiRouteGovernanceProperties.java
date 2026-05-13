package com.tcc.pjb.backend.configs.security.governance;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.api.route-governance")
public class ApiRouteGovernanceProperties {

    private boolean enabled = true;
    private int defaultMaxPageSize = 200;
    private long defaultMaxRequestBytes = 1048576L;
    private int defaultMaxPathSegments = 24;
    private long defaultMaxOffset = 10000L;
    private final List<String> exemptPrefixes = new ArrayList<>(List.of("/actuator/health", "/actuator/health/", "/livez", "/readyz", "/startupz"));
    private final List<Rule> rules = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultMaxPageSize() {
        return defaultMaxPageSize;
    }

    public void setDefaultMaxPageSize(int defaultMaxPageSize) {
        this.defaultMaxPageSize = Math.max(10, defaultMaxPageSize);
    }

    public long getDefaultMaxRequestBytes() {
        return defaultMaxRequestBytes;
    }

    public void setDefaultMaxRequestBytes(long defaultMaxRequestBytes) {
        this.defaultMaxRequestBytes = Math.max(1024L, defaultMaxRequestBytes);
    }

    public int getDefaultMaxPathSegments() {
        return defaultMaxPathSegments;
    }

    public void setDefaultMaxPathSegments(int defaultMaxPathSegments) {
        this.defaultMaxPathSegments = Math.max(4, defaultMaxPathSegments);
    }

    public long getDefaultMaxOffset() {
        return defaultMaxOffset;
    }

    public void setDefaultMaxOffset(long defaultMaxOffset) {
        this.defaultMaxOffset = Math.max(0L, defaultMaxOffset);
    }

    public List<String> getExemptPrefixes() {
        return exemptPrefixes;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public static class Rule {
        private String name;
        private List<String> paths = new ArrayList<>();
        private List<String> methods = new ArrayList<>();
        private List<String> authorities = new ArrayList<>();
        private List<String> allowedContentTypes = new ArrayList<>();
        private int maxPageSize = 0;
        private long maxRequestBytes = 0L;
        private int maxPathSegments = 0;
        private long maxOffset = -1L;
        private long maxRequestsPerWindow = 0L;
        private int rateWindowSeconds = 0;
        private String rateLimitKeyStrategy = "ip";
        private boolean noStoreResponse = false;
        private boolean emitHeaders = true;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name == null ? null : name.trim();
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = copy(paths);
        }

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = copy(methods);
        }

        public List<String> getAuthorities() {
            return authorities;
        }

        public void setAuthorities(List<String> authorities) {
            this.authorities = copy(authorities);
        }

        public List<String> getAllowedContentTypes() {
            return allowedContentTypes;
        }

        public void setAllowedContentTypes(List<String> allowedContentTypes) {
            this.allowedContentTypes = copyNormalized(allowedContentTypes);
        }

        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = Math.max(0, maxPageSize);
        }

        public long getMaxRequestBytes() {
            return maxRequestBytes;
        }

        public void setMaxRequestBytes(long maxRequestBytes) {
            this.maxRequestBytes = Math.max(0L, maxRequestBytes);
        }

        public int getMaxPathSegments() {
            return maxPathSegments;
        }

        public void setMaxPathSegments(int maxPathSegments) {
            this.maxPathSegments = Math.max(0, maxPathSegments);
        }

        public long getMaxOffset() {
            return maxOffset;
        }

        public void setMaxOffset(long maxOffset) {
            this.maxOffset = maxOffset;
        }

        public long getMaxRequestsPerWindow() {
            return maxRequestsPerWindow;
        }

        public void setMaxRequestsPerWindow(long maxRequestsPerWindow) {
            this.maxRequestsPerWindow = Math.max(0L, maxRequestsPerWindow);
        }

        public int getRateWindowSeconds() {
            return rateWindowSeconds;
        }

        public void setRateWindowSeconds(int rateWindowSeconds) {
            this.rateWindowSeconds = Math.max(0, rateWindowSeconds);
        }

        public String getRateLimitKeyStrategy() {
            return rateLimitKeyStrategy;
        }

        public void setRateLimitKeyStrategy(String rateLimitKeyStrategy) {
            this.rateLimitKeyStrategy = rateLimitKeyStrategy == null || rateLimitKeyStrategy.isBlank()
                    ? "ip"
                    : rateLimitKeyStrategy.trim().toLowerCase(Locale.ROOT);
        }

        public boolean isNoStoreResponse() {
            return noStoreResponse;
        }

        public void setNoStoreResponse(boolean noStoreResponse) {
            this.noStoreResponse = noStoreResponse;
        }

        public boolean isEmitHeaders() {
            return emitHeaders;
        }

        public void setEmitHeaders(boolean emitHeaders) {
            this.emitHeaders = emitHeaders;
        }

        private static List<String> copy(List<String> values) {
            if (values == null || values.isEmpty()) {
                return new ArrayList<>();
            }
            Set<String> unique = new LinkedHashSet<>();
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    unique.add(value.trim());
                }
            }
            return new ArrayList<>(unique);
        }

        private static List<String> copyNormalized(List<String> values) {
            if (values == null || values.isEmpty()) {
                return new ArrayList<>();
            }
            Set<String> unique = new LinkedHashSet<>();
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    unique.add(value.trim().toLowerCase(Locale.ROOT));
                }
            }
            return new ArrayList<>(unique);
        }
    }
}
