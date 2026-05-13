package com.tcc.pjb.backend.integration.judicial.financeiro;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IntegracaoJudicialFinanceiraReconciliationJob {

    private final IntegracaoJudicialFinanceiraLifecycleService lifecycleService;
    private final IntegracaoJudicialFinanceiraProperties properties;

    public IntegracaoJudicialFinanceiraReconciliationJob(IntegracaoJudicialFinanceiraLifecycleService lifecycleService,
                                                         IntegracaoJudicialFinanceiraProperties properties) {
        this.lifecycleService = lifecycleService;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${pjb.integracoes.judicial-financeiro.reconciliation-fixed-rate-ms:300000}")
    public void run() {
        if (!properties.enabled()) {
            return;
        }
        lifecycleService.reprocessarFalhas();
    }
}
