package com.tcc.pjb.backend.core.security.abac.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.security.policy")
public class PolicyProperties {

    
    private String version;

    
    private String descriptorResource = "classpath:policies/security/pjb-access-policy.json";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescriptorResource() {
        return descriptorResource;
    }

    public void setDescriptorResource(String descriptorResource) {
        if (descriptorResource != null && !descriptorResource.isBlank()) {
            this.descriptorResource = descriptorResource.trim();
        }
    }
}
