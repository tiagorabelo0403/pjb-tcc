package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.comunicacao.judicial.sefaz-nfe")
public record SefazNfeProperties(
        boolean enabled,
        boolean strictMode,
        boolean cacheEnabled,
        Duration timeout,
        String userAgent,
        Map<String, String> consultaUrlPorUf
) {
    public SefazNfeProperties {
        timeout = timeout != null ? timeout : Duration.ofSeconds(4);
        userAgent = userAgent != null && !userAgent.isBlank() ? userAgent : "PJB-Interceptacao-SefazNFe/1.0";
        consultaUrlPorUf = consultaUrlPorUf != null ? Map.copyOf(consultaUrlPorUf) : Map.of();
    }
}
