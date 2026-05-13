package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.ai.sufficiency")
public class AiSufficiencyPlannerProperties {

    private final Map<String, DomainRules> domains = new HashMap<>();

    public Map<String, DomainRules> getDomains() {
        return domains;
    }

    public static class DomainRules {

        private List<String> defaultRequests = new ArrayList<>();
        private Map<String, List<String>> versionDefaults = new HashMap<>();
        private Map<String, List<String>> capabilityDefaults = new HashMap<>();
        private Map<String, Map<String, List<String>>> capabilityVersion = new HashMap<>();

        public List<String> getDefaultRequests() {
            return defaultRequests;
        }

        public void setDefaultRequests(List<String> defaultRequests) {
            this.defaultRequests = (defaultRequests == null) ? new ArrayList<>() : new ArrayList<>(defaultRequests);
        }

        public Map<String, List<String>> getVersionDefaults() {
            return versionDefaults;
        }

        public void setVersionDefaults(Map<String, List<String>> versionDefaults) {
            this.versionDefaults = (versionDefaults == null) ? new HashMap<>() : new HashMap<>(versionDefaults);
        }

        public Map<String, List<String>> getCapabilityDefaults() {
            return capabilityDefaults;
        }

        public void setCapabilityDefaults(Map<String, List<String>> capabilityDefaults) {
            this.capabilityDefaults = (capabilityDefaults == null) ? new HashMap<>() : new HashMap<>(capabilityDefaults);
        }

        public Map<String, Map<String, List<String>>> getCapabilityVersion() {
            return capabilityVersion;
        }

        public void setCapabilityVersion(Map<String, Map<String, List<String>>> capabilityVersion) {
            this.capabilityVersion = (capabilityVersion == null) ? new HashMap<>() : new HashMap<>(capabilityVersion);
        }
    }
}
