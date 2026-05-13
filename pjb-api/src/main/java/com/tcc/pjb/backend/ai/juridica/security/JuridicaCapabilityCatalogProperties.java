package com.tcc.pjb.backend.ai.juridica.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.security.juridica.capability-catalog")
public class JuridicaCapabilityCatalogProperties {

    
    private boolean denyUnknown = true;

    
    private boolean allowIfMissing = false;

    
    private List<String> allowExact = new ArrayList<>();
    private List<String> allowPrefix = new ArrayList<>();
    private List<String> allowContainsAny = new ArrayList<>();

    public boolean isDenyUnknown() {
        return denyUnknown;
    }

    public void setDenyUnknown(boolean denyUnknown) {
        this.denyUnknown = denyUnknown;
    }

    public boolean isAllowIfMissing() {
        return allowIfMissing;
    }

    public void setAllowIfMissing(boolean allowIfMissing) {
        this.allowIfMissing = allowIfMissing;
    }

    public List<String> getAllowExact() {
        return allowExact;
    }

    public void setAllowExact(List<String> allowExact) {
        this.allowExact = (allowExact != null) ? allowExact : new ArrayList<>();
    }

    public List<String> getAllowPrefix() {
        return allowPrefix;
    }

    public void setAllowPrefix(List<String> allowPrefix) {
        this.allowPrefix = (allowPrefix != null) ? allowPrefix : new ArrayList<>();
    }

    public List<String> getAllowContainsAny() {
        return allowContainsAny;
    }

    public void setAllowContainsAny(List<String> allowContainsAny) {
        this.allowContainsAny = (allowContainsAny != null) ? allowContainsAny : new ArrayList<>();
    }
}
