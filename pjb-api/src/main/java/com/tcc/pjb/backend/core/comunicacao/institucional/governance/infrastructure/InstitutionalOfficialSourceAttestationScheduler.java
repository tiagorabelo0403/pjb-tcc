package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceAttestationApplicationService;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import java.util.Objects;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InstitutionalOfficialSourceAttestationScheduler {

    private final InstitutionalOfficialSourceAttestationApplicationService applicationService;
    private final boolean enabled;
    private final int batchSize;

    public InstitutionalOfficialSourceAttestationScheduler(InstitutionalOfficialSourceAttestationApplicationService applicationService,
                                                           Environment environment) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.enabled = Boolean.parseBoolean(environment.getProperty("pjb.institutional.official-source.scheduler.enabled", "true"));
        this.batchSize = Integer.parseInt(environment.getProperty("pjb.institutional.official-source.scheduler.batch", "24"));
    }

    @PjbClusterSingletonTask(key = "institutional-official-source-attestation", ttl = "PT10M")
    @Scheduled(fixedDelayString = "${pjb.institutional.official-source.scheduler.delay-ms:60000}")
    public void tick() {
        if (!enabled) {
            return;
        }
        applicationService.revalidarPendencias(batchSize);
    }
}
