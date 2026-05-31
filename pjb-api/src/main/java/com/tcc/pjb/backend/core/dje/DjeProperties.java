package com.tcc.pjb.backend.core.dje;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.dje")
public record DjeProperties(
        boolean enabled,
        boolean mockEnabled,
        long schedulerFixedRateMs,
        int maxBatchSize
) {
}
