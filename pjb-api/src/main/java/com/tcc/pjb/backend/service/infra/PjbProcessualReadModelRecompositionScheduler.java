package com.tcc.pjb.backend.service.infra;

import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelRecompositionJob;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.datasource.routing.processual-read-models", name = "persistence-enabled", havingValue = "true", matchIfMissing = true)
public class PjbProcessualReadModelRecompositionScheduler {

    private final PjbProcessualReadModelRecompositionQueueService queueService;
    private final PjbProcessualReadModelPersistenceService persistenceService;
    private final PjbDataSourceRoutingProperties properties;

    public PjbProcessualReadModelRecompositionScheduler(PjbProcessualReadModelRecompositionQueueService queueService,
                                                        PjbProcessualReadModelPersistenceService persistenceService,
                                                        PjbDataSourceRoutingProperties properties) {
        this.queueService = Objects.requireNonNull(queueService, "queueService");
        this.persistenceService = Objects.requireNonNull(persistenceService, "persistenceService");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Scheduled(fixedDelayString = "${pjb.datasource.routing.processual-read-models.recomposition-poll-ms:20000}")
    public void replay() {
        if (!properties.getProcessualReadModels().isPersistenceEnabled()) {
            return;
        }
        List<ProcessualReadModelRecompositionJob> jobs = queueService.claimBatch();
        for (ProcessualReadModelRecompositionJob job : jobs) {
            if (job == null || job.getId() == null) {
                continue;
            }
            try {
                PjbProcessualReadModelPersistenceService.RecompositionResult result = persistenceService.recompose(
                        job.getDomain(),
                        job.getTribunalCode(),
                        job.getRamoCode(),
                        job.getScopeKey(),
                        "RECOMPOSITION_SCHEDULER",
                        job.getReason()
                );
                queueService.complete(job.getId(), result.status() + ":" + result.affectedProjections());
            } catch (Exception ex) {
                queueService.fail(job.getId(), ex.getMessage());
            }
        }
    }
}
