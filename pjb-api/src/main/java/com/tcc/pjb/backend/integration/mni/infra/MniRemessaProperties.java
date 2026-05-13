package com.tcc.pjb.backend.integration.mni.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.mni")
public record MniRemessaProperties(
        boolean enabled,
        int retryMaxTentativas,
        long reconciliationFixedRateMs,
        int reconciliationBatchSize
) {
}
