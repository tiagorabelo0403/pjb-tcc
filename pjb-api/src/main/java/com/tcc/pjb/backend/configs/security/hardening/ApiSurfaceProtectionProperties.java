package com.tcc.pjb.backend.configs.security.hardening;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.api.surface")
public class ApiSurfaceProtectionProperties {

    private boolean enabled = true;
    private int maxRequestUriLength = 4096;
    private int maxQueryStringLength = 4096;
    private int maxHeaderCount = 120;
    private int maxHeaderValueLength = 4096;
    private int maxParameterCount = 200;
    private int maxRequestIdLength = 80;
    private boolean rejectBackslashPath = true;
    private boolean rejectPathTraversalTokens = true;
    private boolean rejectDoubleSlashPath = true;
    private final List<String> exemptPrefixes = new ArrayList<>(List.of("/actuator/health", "/actuator/health/", "/livez", "/readyz", "/startupz"));

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getMaxRequestUriLength() { return maxRequestUriLength; }
    public void setMaxRequestUriLength(int v) { this.maxRequestUriLength = Math.max(256, v); }
    public int getMaxQueryStringLength() { return maxQueryStringLength; }
    public void setMaxQueryStringLength(int v) { this.maxQueryStringLength = Math.max(256, v); }
    public int getMaxHeaderCount() { return maxHeaderCount; }
    public void setMaxHeaderCount(int v) { this.maxHeaderCount = Math.max(1, v); }
    public int getMaxHeaderValueLength() { return maxHeaderValueLength; }
    public void setMaxHeaderValueLength(int v) { this.maxHeaderValueLength = Math.max(128, v); }
    public int getMaxParameterCount() { return maxParameterCount; }
    public void setMaxParameterCount(int v) { this.maxParameterCount = Math.max(8, v); }
    public int getMaxRequestIdLength() { return maxRequestIdLength; }
    public void setMaxRequestIdLength(int v) { this.maxRequestIdLength = Math.max(16, v); }
    public boolean isRejectBackslashPath() { return rejectBackslashPath; }
    public void setRejectBackslashPath(boolean rejectBackslashPath) { this.rejectBackslashPath = rejectBackslashPath; }
    public boolean isRejectPathTraversalTokens() { return rejectPathTraversalTokens; }
    public void setRejectPathTraversalTokens(boolean rejectPathTraversalTokens) { this.rejectPathTraversalTokens = rejectPathTraversalTokens; }
    public boolean isRejectDoubleSlashPath() { return rejectDoubleSlashPath; }
    public void setRejectDoubleSlashPath(boolean rejectDoubleSlashPath) { this.rejectDoubleSlashPath = rejectDoubleSlashPath; }
    public List<String> getExemptPrefixes() { return exemptPrefixes; }
}
