package com.tcc.pjb.backend.core.processo.conclusao.application;

import jakarta.inject.Inject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.jobs.conclusao-expirada",
        name = "enabled", havingValue = "true", matchIfMissing = false)
public class ConclusaoExpiradaScheduler {

    private final ConclusaoProcessualApplicationService conclusaoService;

    @Inject
    public ConclusaoExpiradaScheduler(ConclusaoProcessualApplicationService conclusaoService) {
        this.conclusaoService = conclusaoService;
    }

    @Scheduled(cron = "0 0 7 * * MON-FRI", zone = "America/Sao_Paulo")
    public void processarExpiradas() {
        conclusaoService.processarExpiradas();
    }
}
