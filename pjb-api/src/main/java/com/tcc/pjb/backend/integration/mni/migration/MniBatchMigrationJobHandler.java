package com.tcc.pjb.backend.integration.mni.migration;

import com.tcc.pjb.backend.core.backfill.service.BackfillRunService;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;
import com.tcc.pjb.backend.core.jobs.runtime.JobExecutionContext;
import com.tcc.pjb.backend.core.jobs.runtime.JobHandler;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Job assíncrono que drena {@link MniMigrationBatchItem} PENDENTE em lotes, reaproveitando o
 * mesmo framework de progresso resumível de {@code AdvClienteCanonicalizeSensitiveJobHandler}
 * (BackfillRun). Sem dry-run: {@code MniRecepcaoService.receberAutos()} sempre persiste de verdade
 * — não há modo de simulação nessa camada, então este job não finge oferecer um.
 */
@Component
public class MniBatchMigrationJobHandler implements JobHandler {

    private final MniMigrationBatchService service;
    private final JobRepository jobRepository;
    private final BackfillRunService backfillRunService;

    public MniBatchMigrationJobHandler(MniMigrationBatchService service,
                                       JobRepository jobRepository,
                                       BackfillRunService backfillRunService) {
        this.service = Objects.requireNonNull(service);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.backfillRunService = Objects.requireNonNull(backfillRunService);
    }

    @Override
    public JobType type() {
        return JobType.MNI_BATCH_MIGRATION;
    }

    public record Input(Integer batchSize, Long afterId, Long untilId) {
    }

    @Override
    public void execute(JobExecutionContext ctx) {
        Input in = ctx.inputAs(Input.class);
        int batchSize = in.batchSize() != null ? in.batchSize() : 50;
        long afterId = in.afterId() != null ? Math.max(0L, in.afterId()) : 0L;
        Long untilId = in.untilId();

        backfillRunService.upsertKickoff(
                ctx.jobId(),
                JobType.MNI_BATCH_MIGRATION.name(),
                ctx.job().getInboxKey(),
                ctx.job().getOwnerUserId(),
                batchSize,
                false,
                afterId,
                untilId
        );
        backfillRunService.markRunning(ctx.jobId());

        try {
            long total = service.countPending(afterId, untilId);
            long processed = 0L;
            ctx.progress(processed, total);
            jobRepository.save(ctx.job());

            long cursor = afterId;

            while (true) {
                List<Long> pendingIds = service.findPendingIds(cursor, untilId, batchSize);
                if (pendingIds.isEmpty()) {
                    break;
                }

                long batchProcessed = 0L;
                long batchUpdated = 0L;
                long batchDuplicates = 0L;
                for (Long itemId : pendingIds) {
                    batchProcessed++;
                    cursor = Math.max(cursor, itemId);
                    try {
                        MniMigrationBatchService.ItemOutcome outcome = service.processarUmItem(itemId);
                        if (outcome.jaExistiaAntes()) {
                            batchDuplicates++;
                        } else {
                            batchUpdated++;
                        }
                    } catch (Exception e) {
                        service.marcarFalhou(itemId, e.getMessage());
                    }
                }

                processed += batchProcessed;
                backfillRunService.recordBatch(ctx.jobId(), batchProcessed, batchUpdated, batchDuplicates, cursor);
                ctx.progress(processed, total);
                ctx.heartbeat();
                jobRepository.save(ctx.job());

                if (pendingIds.size() < batchSize) {
                    break;
                }
            }

            ctx.progress(total > 0 ? Math.min(processed, total) : processed, total);
            jobRepository.save(ctx.job());
            backfillRunService.markSucceeded(ctx.jobId());
        } catch (Exception e) {
            backfillRunService.markFailed(ctx.jobId(), e.getMessage());
            throw e;
        }
    }
}
