package com.tcc.pjb.backend.modules.atendimento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.atendimento.retention")
public class AtendimentoRetentionProperties {

    private int attachmentDays = 30;

    public int getAttachmentDays() {
        return attachmentDays;
    }

    public void setAttachmentDays(int attachmentDays) {
        this.attachmentDays = attachmentDays;
    }
}
