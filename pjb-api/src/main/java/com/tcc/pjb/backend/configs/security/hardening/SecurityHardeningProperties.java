package com.tcc.pjb.backend.configs.security.hardening;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.security.hardening")
public class SecurityHardeningProperties {

    private boolean enabled = true;
    private boolean rejectTrace = true;
    private boolean rejectTrack = true;
    private boolean rejectConnect = true;
    private boolean rejectMethodOverrideHeaders = true;
    private boolean addBrowserHardeningHeaders = true;
    private boolean noStoreSensitiveResponses = true;
    private final List<String> sensitivePaths = new ArrayList<>(List.of(
            "/api/admin/",
            "/api/v1/admin/",
            "/actuator/",
            "/api/v1/security/",
            "/api/v1/auth/"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRejectTrace() {
        return rejectTrace;
    }

    public void setRejectTrace(boolean rejectTrace) {
        this.rejectTrace = rejectTrace;
    }

    public boolean isRejectTrack() {
        return rejectTrack;
    }

    public void setRejectTrack(boolean rejectTrack) {
        this.rejectTrack = rejectTrack;
    }

    public boolean isRejectConnect() {
        return rejectConnect;
    }

    public void setRejectConnect(boolean rejectConnect) {
        this.rejectConnect = rejectConnect;
    }

    public boolean isRejectMethodOverrideHeaders() {
        return rejectMethodOverrideHeaders;
    }

    public void setRejectMethodOverrideHeaders(boolean rejectMethodOverrideHeaders) {
        this.rejectMethodOverrideHeaders = rejectMethodOverrideHeaders;
    }

    public boolean isAddBrowserHardeningHeaders() {
        return addBrowserHardeningHeaders;
    }

    public void setAddBrowserHardeningHeaders(boolean addBrowserHardeningHeaders) {
        this.addBrowserHardeningHeaders = addBrowserHardeningHeaders;
    }

    public boolean isNoStoreSensitiveResponses() {
        return noStoreSensitiveResponses;
    }

    public void setNoStoreSensitiveResponses(boolean noStoreSensitiveResponses) {
        this.noStoreSensitiveResponses = noStoreSensitiveResponses;
    }

    public List<String> getSensitivePaths() {
        return sensitivePaths;
    }
}
