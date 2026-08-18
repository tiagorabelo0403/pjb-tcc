package com.tcc.pjb.backend.core.comunicacao.judicial;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.bnmp")
public record PjbBnmpProperties(
        boolean enabled,
        boolean mockEnabled,
        int timeoutSegundos
) {
    public PjbBnmpProperties {
        timeoutSegundos = timeoutSegundos > 0 ? timeoutSegundos : 15;
    }
}
