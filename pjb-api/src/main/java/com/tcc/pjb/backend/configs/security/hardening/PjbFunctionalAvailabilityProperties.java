package com.tcc.pjb.backend.configs.security.hardening;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalDomain;

@ConfigurationProperties(prefix = "pjb.api.functional-availability")
public class PjbFunctionalAvailabilityProperties {

    private boolean enabled = true;
    private int rejectionStatus = 503;
    private boolean emitDebugHeaders = false;
    private final EnumMap<PjbFunctionalDomain, Boolean> domains = new EnumMap<>(PjbFunctionalDomain.class);

    public PjbFunctionalAvailabilityProperties() {
        for (PjbFunctionalDomain domain : PjbFunctionalDomain.values()) {
            domains.put(domain, Boolean.TRUE);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public Map<PjbFunctionalDomain, Boolean> getDomains() {
        return domains;
    }

    public boolean isAvailable(PjbFunctionalDomain domain) {
        if (domain == null) {
            return true;
        }
        return Boolean.TRUE.equals(domains.getOrDefault(domain, Boolean.TRUE));
    }
}
