package com.tcc.pjb.backend.core.processo.ciencia.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "pjb.jobs.ciencia-ficta",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false)
public class CienciaFictaScheduler {

    private final CienciaProcessualApplicationService cienciaService;

    public CienciaFictaScheduler(CienciaProcessualApplicationService cienciaService) {
        this.cienciaService = cienciaService;
    }

    @Scheduled(cron = "0 0 6 * * MON-FRI", zone = "America/Sao_Paulo")
    public void processarCienciasFictas() {
        cienciaService.processarExpirados();
    }
}
