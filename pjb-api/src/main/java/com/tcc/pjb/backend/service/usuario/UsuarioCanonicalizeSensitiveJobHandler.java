package com.tcc.pjb.backend.service.usuario;

import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.backfill.service.BackfillRunService;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;
import com.tcc.pjb.backend.core.jobs.runtime.JobExecutionContext;
import com.tcc.pjb.backend.core.jobs.runtime.JobHandler;

/**
 * Backfill de cifragem/índice cego de {@code tb_usuario.cpf}/{@code email} (V344) — mesmo padrão de
 * job já usado em {@code ADV_CLIENTE_CANONICALIZE_SENSITIVE}.
 */
@Component
public class UsuarioCanonicalizeSensitiveJobHandler implements JobHandler {

    private final UsuarioCanonicalizeSensitiveService service;
    private final JobRepository jobRepository;
    private final BackfillRunService backfillRunService;

    public UsuarioCanonicalizeSensitiveJobHandler(UsuarioCanonicalizeSensitiveService service,
                                                  JobRepository jobRepository,
                                                  BackfillRunService backfillRunService) {
        this.service = Objects.requireNonNull(service);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.backfillRunService = Objects.requireNonNull(backfillRunService);
    }

    @Override
    public JobType type() {
        return JobType.USUARIO_CANONICALIZE_SENSITIVE;
    }

    public record Input(Integer batchSize, Boolean dryRun, Long afterId, Long untilId) {
    }

    @Override
    public void execute(JobExecutionContext ctx) {
        Input in = ctx.inputAs(Input.class);

        int batchSize = in.batchSize() != null ? in.batchSize() : 500;
        boolean dryRun = in.dryRun() != null && in.dryRun();
        long afterId = in.afterId() != null ? Math.max(0L, in.afterId()) : 0L;
        Long untilId = in.untilId();

        backfillRunService.upsertKickoff(
                ctx.jobId(),
                JobType.USUARIO_CANONICALIZE_SENSITIVE.name(),
                ctx.job().getInboxKey(),
                ctx.job().getOwnerUserId(),
                batchSize,
                dryRun,
                afterId,
                untilId
        );
        backfillRunService.markRunning(ctx.jobId());

        try {
            long total = service.countTotal(afterId, untilId);
            long processed = 0L;

            ctx.progress(processed, total);
            jobRepository.save(ctx.job());

            long cursor = afterId;

            while (true) {
                UsuarioCanonicalizeSensitiveService.BatchResult r = service.canonicalizeBatch(cursor, untilId, batchSize, dryRun);
                if (r.processed() <= 0) {
                    break;
                }

                processed += r.processed();
                cursor = r.lastId();
                backfillRunService.recordBatch(ctx.jobId(), r.processed(), r.updated(), 0L, cursor);

                ctx.progress(processed, total);
                ctx.heartbeat();
                jobRepository.save(ctx.job());

                if (r.done()) {
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
