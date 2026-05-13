package com.tcc.pjb.backend.integration.judicial.financeiro;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.integracoes.judicial-financeiro")
public record IntegracaoJudicialFinanceiraProperties(
        boolean enabled,
        boolean dryRun,
        int retryMaxTentativas,
        long reconciliationFixedRateMs,
        int reconciliationBatchSize,
        long retryBackoffMinutes
) {
}
