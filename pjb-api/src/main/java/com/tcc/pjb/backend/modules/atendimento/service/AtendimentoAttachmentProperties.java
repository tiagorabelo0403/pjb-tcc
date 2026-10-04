package com.tcc.pjb.backend.modules.atendimento.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.atendimento.attachments")
public class AtendimentoAttachmentProperties {

    private boolean enabled = false;
    private int maxPerMessage = 3;
    private long maxTotalBytesPerMessage = 20_971_520L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxPerMessage() {
        return maxPerMessage;
    }

    public void setMaxPerMessage(int maxPerMessage) {
        this.maxPerMessage = maxPerMessage;
    }

    public long getMaxTotalBytesPerMessage() {
        return maxTotalBytesPerMessage;
    }

    public void setMaxTotalBytesPerMessage(long maxTotalBytesPerMessage) {
        this.maxTotalBytesPerMessage = maxTotalBytesPerMessage;
    }
}
