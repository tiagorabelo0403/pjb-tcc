package com.tcc.pjb.backend.platform.security.ratelimit;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.security.capability-ratelimit")
public class CapabilityRateLimitProperties {

    private boolean enabled = true;
    
    private String store = "local";
    private String keyPrefix = "pjb:caprl";
    private int windowSeconds = 60;
    private int defaultLimitTokens = 60;

    
    private VersionLimitDefaults versionDefaultLimitTokens = new VersionLimitDefaults();

    private VersionCost versionCost = new VersionCost();
    private Map<String, DomainPolicy> domains = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getDefaultLimitTokens() {
        return defaultLimitTokens;
    }

    public void setDefaultLimitTokens(int defaultLimitTokens) {
        this.defaultLimitTokens = defaultLimitTokens;
    }

    public VersionCost getVersionCost() {
        return versionCost;
    }

    public void setVersionCost(VersionCost versionCost) {
        this.versionCost = versionCost;
    }

    public VersionLimitDefaults getVersionDefaultLimitTokens() {
        return versionDefaultLimitTokens;
    }

    public void setVersionDefaultLimitTokens(VersionLimitDefaults versionDefaultLimitTokens) {
        this.versionDefaultLimitTokens = (versionDefaultLimitTokens != null) ? versionDefaultLimitTokens : new VersionLimitDefaults();
    }

    public Map<String, DomainPolicy> getDomains() {
        return domains;
    }

    public void setDomains(Map<String, DomainPolicy> domains) {
        this.domains = (domains != null) ? new LinkedHashMap<>(domains) : new LinkedHashMap<>();
    }

    public DomainPolicy domainPolicy(CapabilityRateLimitDomain domain) {
        if (domain == null) return null;
        if (domains == null || domains.isEmpty()) return null;

        DomainPolicy p = domains.get(domain.name());
        if (p != null) return p;

        p = domains.get(domain.canonical());
        if (p != null) return p;

        
        String dn = domain.name().toLowerCase(Locale.ROOT);
        return domains.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().trim().toLowerCase(Locale.ROOT).equals(dn))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static class VersionCost {
        private int v1 = 1;
        private int v2 = 3;
        private int v3 = 5;

        public int getV1() {
            return v1;
        }

        public void setV1(int v1) {
            this.v1 = v1;
        }

        public int getV2() {
            return v2;
        }

        public void setV2(int v2) {
            this.v2 = v2;
        }

        public int getV3() {
            return v3;
        }

        public void setV3(int v3) {
            this.v3 = v3;
        }
    }

    
    public static class VersionLimitDefaults {
        private Integer v1;
        private Integer v2;
        private Integer v3;

        public Integer getV1() {
            return v1;
        }

        public void setV1(Integer v1) {
            this.v1 = v1;
        }

        public Integer getV2() {
            return v2;
        }

        public void setV2(Integer v2) {
            this.v2 = v2;
        }

        public Integer getV3() {
            return v3;
        }

        public void setV3(Integer v3) {
            this.v3 = v3;
        }
    }

    public static class DomainPolicy {
        private Integer defaultLimitTokens;
        private Map<String, Integer> exact = new LinkedHashMap<>();
        private Map<String, Integer> prefix = new LinkedHashMap<>();

        
        private Map<String, VersionPolicy> versions = new LinkedHashMap<>();

        public Integer getDefaultLimitTokens() {
            return defaultLimitTokens;
        }

        public void setDefaultLimitTokens(Integer defaultLimitTokens) {
            this.defaultLimitTokens = defaultLimitTokens;
        }

        public Map<String, Integer> getExact() {
            return exact;
        }

        public void setExact(Map<String, Integer> exact) {
            this.exact = (exact != null) ? new LinkedHashMap<>(exact) : new LinkedHashMap<>();
        }

        public Map<String, Integer> getPrefix() {
            return prefix;
        }

        public void setPrefix(Map<String, Integer> prefix) {
            this.prefix = (prefix != null) ? new LinkedHashMap<>(prefix) : new LinkedHashMap<>();
        }

        public Map<String, VersionPolicy> getVersions() {
            return versions;
        }

        public void setVersions(Map<String, VersionPolicy> versions) {
            this.versions = (versions != null) ? new LinkedHashMap<>(versions) : new LinkedHashMap<>();
        }
    }

    public static class VersionPolicy {
        private Integer defaultLimitTokens;
        private Map<String, Integer> exact = new LinkedHashMap<>();
        private Map<String, Integer> prefix = new LinkedHashMap<>();

        public Integer getDefaultLimitTokens() {
            return defaultLimitTokens;
        }

        public void setDefaultLimitTokens(Integer defaultLimitTokens) {
            this.defaultLimitTokens = defaultLimitTokens;
        }

        public Map<String, Integer> getExact() {
            return exact;
        }

        public void setExact(Map<String, Integer> exact) {
            this.exact = (exact != null) ? new LinkedHashMap<>(exact) : new LinkedHashMap<>();
        }

        public Map<String, Integer> getPrefix() {
            return prefix;
        }

        public void setPrefix(Map<String, Integer> prefix) {
            this.prefix = (prefix != null) ? new LinkedHashMap<>(prefix) : new LinkedHashMap<>();
        }
    }
}
