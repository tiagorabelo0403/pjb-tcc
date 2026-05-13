package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure;

import java.util.Objects;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.application.InstitutionalDeliveryQueueApplicationService;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;

@Component
public class InstitutionalDeliveryScheduler {

    private final InstitutionalDeliveryQueueApplicationService queueService;
    private final boolean enabled;
    private final int batchSize;

    public InstitutionalDeliveryScheduler(InstitutionalDeliveryQueueApplicationService queueService,
                                          Environment environment) {
        this.queueService = Objects.requireNonNull(queueService);
        this.enabled = Boolean.parseBoolean(environment.getProperty("pjb.institutional.delivery.scheduler.enabled", "true"));
        this.batchSize = Integer.parseInt(environment.getProperty("pjb.institutional.delivery.scheduler.batch", "32"));
    }

    @PjbClusterSingletonTask(key = "institutional-delivery-poller", ttl = "PT30S")
    @Scheduled(fixedDelayString = "${pjb.institutional.delivery.scheduler.delay-ms:1500}")
    public void tick() {
        if (!enabled) {
            return;
        }
        queueService.processarPendencias(batchSize);
    }
}
