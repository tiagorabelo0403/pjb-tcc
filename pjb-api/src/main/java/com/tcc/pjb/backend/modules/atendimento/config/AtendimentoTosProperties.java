package com.tcc.pjb.backend.modules.atendimento.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.atendimento.tos")
public class AtendimentoTosProperties {

    private int version = 1;
    private String url = "";

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
