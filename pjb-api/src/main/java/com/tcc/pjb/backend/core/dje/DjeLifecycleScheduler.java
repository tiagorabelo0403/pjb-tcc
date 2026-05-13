package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.core.dje.domain.DjeLifecycleCommand;
import com.tcc.pjb.backend.core.dje.domain.DjeNotificarPartesCommand;
import com.tcc.pjb.backend.core.dje.domain.DjeConsolidarPublicadasCommand;
import com.tcc.pjb.backend.core.dje.domain.DjeLifecycleExecutionSummary;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DjeLifecycleScheduler {

    private final DjePublicacaoService service;
    private final DjeProperties properties;

    public DjeLifecycleScheduler(DjePublicacaoService service,
                                 DjeProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${pjb.dje.scheduler-fixed-rate-ms:300000}")
    public void run() {
        if (!properties.enabled()) {
            return;
        }
        LocalDate hoje = LocalDate.now();
        int consolidadas = service.consolidarPublicadas(new DjeConsolidarPublicadasCommand(hoje, properties.maxBatchSize())).totalConsolidadas();
        int notificadas = service.notificarPartesPublicadas(new DjeNotificarPartesCommand(hoje, properties.maxBatchSize())).totalNotificadas();
        DjeLifecycleExecutionSummary summary = new DjeLifecycleExecutionSummary(consolidadas, notificadas);
        if (summary.publicadasConsolidadas() < 0 || summary.partesNotificadas() < 0) {
            throw new IllegalStateException("Resumo DJe inválido");
        }
    }
}
