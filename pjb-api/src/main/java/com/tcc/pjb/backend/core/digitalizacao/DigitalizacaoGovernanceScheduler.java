package com.tcc.pjb.backend.core.digitalizacao;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DigitalizacaoGovernanceScheduler {

    private final DigitalizacaoGovernanceService governanceService;
    private final DigitalizacaoProperties properties;

    public DigitalizacaoGovernanceScheduler(DigitalizacaoGovernanceService governanceService,
                                            DigitalizacaoProperties properties) {
        this.governanceService = governanceService;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${pjb.digitalizacao.governance-fixed-rate-ms:300000}")
    public void run() {
        if (!properties.enabled()) {
            return;
        }
        governanceService.marcarProcessamentosEstagnados();
    }
}
