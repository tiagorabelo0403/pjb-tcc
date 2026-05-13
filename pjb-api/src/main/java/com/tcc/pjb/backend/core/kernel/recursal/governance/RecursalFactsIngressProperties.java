package com.tcc.pjb.backend.core.kernel.recursal.governance;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.recursal.facts")
public class RecursalFactsIngressProperties {


    private boolean payloadLimitEnabled = true;


    private long maxRequestBytes = 2_097_152L;

    public boolean isPayloadLimitEnabled() {
        return payloadLimitEnabled;
    }

    public void setPayloadLimitEnabled(boolean payloadLimitEnabled) {
        this.payloadLimitEnabled = payloadLimitEnabled;
    }

    public long getMaxRequestBytes() {
        return maxRequestBytes;
    }

    public void setMaxRequestBytes(long maxRequestBytes) {
        if (maxRequestBytes <= 0) {
            this.maxRequestBytes = 2_097_152L;
            return;
        }
        this.maxRequestBytes = maxRequestBytes;
    }
}
