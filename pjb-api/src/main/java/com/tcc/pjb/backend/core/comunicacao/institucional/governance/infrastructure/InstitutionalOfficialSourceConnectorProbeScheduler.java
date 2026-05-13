package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceConnectorProbeApplicationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InstitutionalOfficialSourceConnectorProbeScheduler {

    private final InstitutionalOfficialSourceConnectorProbeApplicationService probeApplicationService;
    private final InstitutionalOfficialSourceConnectorProperties properties;

    public InstitutionalOfficialSourceConnectorProbeScheduler(InstitutionalOfficialSourceConnectorProbeApplicationService probeApplicationService,
                                                              InstitutionalOfficialSourceConnectorProperties properties) {
        this.probeApplicationService = probeApplicationService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${pjb.institutional.official-source.connector-probe.delay-ms:1800000}")
    public void sondar() {
        probeApplicationService.sondarRecorrencia(Math.max(1, properties.getProbeBatchSize()));
    }
}
