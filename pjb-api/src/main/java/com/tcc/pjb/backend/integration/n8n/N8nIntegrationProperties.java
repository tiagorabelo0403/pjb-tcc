package com.tcc.pjb.backend.integration.n8n;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.integration.n8n")
public class N8nIntegrationProperties {

    private boolean enabled;
    private String baseUrl;
    private String dispatchPath = "/webhook/pjb-event-bus";
    private String dispatchSecret;
    private String inboundSecret;
    private String tenant = "pjb";
    private boolean requireHttps = true;
    private boolean allowLocalHttp = true;
    private boolean failOnDispatchError;
    private Duration requestTimeout = Duration.ofSeconds(15);
    private int maxPayloadBytes = 262144;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getDispatchPath() { return dispatchPath; }
    public void setDispatchPath(String dispatchPath) { this.dispatchPath = dispatchPath; }

    public String getDispatchSecret() { return dispatchSecret; }
    public void setDispatchSecret(String dispatchSecret) { this.dispatchSecret = dispatchSecret; }

    public String getInboundSecret() { return inboundSecret; }
    public void setInboundSecret(String inboundSecret) { this.inboundSecret = inboundSecret; }

    public String getTenant() { return tenant; }
    public void setTenant(String tenant) { this.tenant = tenant; }

    public boolean isRequireHttps() { return requireHttps; }
    public void setRequireHttps(boolean requireHttps) { this.requireHttps = requireHttps; }

    public boolean isAllowLocalHttp() { return allowLocalHttp; }
    public void setAllowLocalHttp(boolean allowLocalHttp) { this.allowLocalHttp = allowLocalHttp; }

    public boolean isFailOnDispatchError() { return failOnDispatchError; }
    public void setFailOnDispatchError(boolean failOnDispatchError) { this.failOnDispatchError = failOnDispatchError; }

    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout == null ? Duration.ofSeconds(15) : requestTimeout; }

    public int getMaxPayloadBytes() { return maxPayloadBytes; }
    public void setMaxPayloadBytes(int maxPayloadBytes) { this.maxPayloadBytes = Math.max(16384, maxPayloadBytes); }
}
