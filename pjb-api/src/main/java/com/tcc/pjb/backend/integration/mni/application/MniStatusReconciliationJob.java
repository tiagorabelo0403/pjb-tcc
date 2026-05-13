package com.tcc.pjb.backend.integration.mni.application;

import com.tcc.pjb.backend.integration.mni.infra.MniRemessaProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MniStatusReconciliationJob {

    private final MniRemessaService service;
    private final MniRemessaProperties properties;

    public MniStatusReconciliationJob(MniRemessaService service,
                                      MniRemessaProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${pjb.mni.reconciliation-fixed-rate-ms:300000}")
    public void run() {
        if (!properties.enabled()) {
            return;
        }
        service.reprocessarPendentes();
    }
}
