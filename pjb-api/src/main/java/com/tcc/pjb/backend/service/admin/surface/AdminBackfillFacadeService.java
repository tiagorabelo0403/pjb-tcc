package com.tcc.pjb.backend.service.admin.surface;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.backfill.service.BackfillRunService;
import com.tcc.pjb.backend.core.jobs.domain.JobStatus;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;
import com.tcc.pjb.backend.core.jobs.runtime.JobCommandService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.mni.migration.MniBatchMigrationJobHandler;
import com.tcc.pjb.backend.integration.mni.migration.MniMigrationBatchService;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminBackfillCanonicalizeSensitiveRequest;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminBackfillKickoffResponse;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminBackfillMniMigrationRequest;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminBackfillStatusResponse;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminMniMigrationEnqueueRequest;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminMniMigrationEnqueueResponse;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminMniMigrationFailedItemDto;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminMniMigrationItemRequest;
import com.tcc.pjb.backend.modules.advocacia.jobs.AdvClienteCanonicalizeSensitiveJobHandler;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AdminBackfillFacadeService {

    private final JobCommandService jobCommandService;
    private final JobRepository jobRepository;
    private final BackfillRunService backfillRunService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final MniMigrationBatchService mniMigrationBatchService;

    public AdminBackfillFacadeService(JobCommandService jobCommandService,
                                      JobRepository jobRepository,
                                      BackfillRunService backfillRunService,
                                      CurrentUserService currentUserService,
                                      ObjectMapper objectMapper,
                                      MniMigrationBatchService mniMigrationBatchService) {
        this.jobCommandService = Objects.requireNonNull(jobCommandService);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.backfillRunService = Objects.requireNonNull(backfillRunService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.mniMigrationBatchService = Objects.requireNonNull(mniMigrationBatchService);
    }

    public AdminBackfillKickoffResponse kickoffCanonicalizeSensitive(AdminBackfillCanonicalizeSensitiveRequest request,
                                                                     String idempotencyKey,
                                                                     String clientRequestId) {
        String owner = safeOwner();
        String dedupeKey = firstNonBlank(idempotencyKey, clientRequestId)
                .orElseGet(() -> buildCanonicalKey(request));
        String inboxKey = request.inboxKey() != null && !request.inboxKey().isBlank()
                ? request.inboxKey().trim()
                : "advocacia:clientes:canonicalizeSensitive";
        int priority = request.priority() != null ? Math.max(0, request.priority()) : 0;
        int maxAttempts = request.maxAttempts() != null ? Math.max(1, request.maxAttempts()) : 3;
        int batchSize = request.batchSize() != null ? request.batchSize() : 500;
        AdvClienteCanonicalizeSensitiveJobHandler.Input input = new AdvClienteCanonicalizeSensitiveJobHandler.Input(
                batchSize,
                request.dryRun(),
                request.afterId(),
                request.untilId()
        );
        JsonNode inputNode = objectMapper.valueToTree(input);
        JobCommandService.JobCreateResult result = jobCommandService.createIdempotent(
                JobType.ADV_CLIENTE_CANONICALIZE_SENSITIVE,
                inboxKey,
                owner,
                dedupeKey,
                inputNode,
                priority,
                maxAttempts
        );
        backfillRunService.upsertKickoff(
                result.jobId(),
                JobType.ADV_CLIENTE_CANONICALIZE_SENSITIVE.name(),
                inboxKey,
                owner,
                batchSize,
                request.dryRun() != null && request.dryRun(),
                request.afterId() != null ? Math.max(0L, request.afterId()) : 0L,
                request.untilId()
        );
        return new AdminBackfillKickoffResponse(result.jobId(), JobStatus.PENDING.name(), result.replay(), result.inProgress());
    }

    public Optional<AdminBackfillStatusResponse> canonicalizeStatus(UUID jobId, String inboxKey) {
        String resolvedInboxKey = inboxKey != null && !inboxKey.isBlank()
                ? inboxKey.trim()
                : "advocacia:clientes:canonicalizeSensitive";
        var runOpt = jobId != null
                ? backfillRunService.findById(jobId)
                : backfillRunService.findLatest(JobType.ADV_CLIENTE_CANONICALIZE_SENSITIVE.name(), resolvedInboxKey);
        if (runOpt.isEmpty()) {
            return Optional.empty();
        }
        var run = runOpt.get();
        var jobOpt = jobRepository.findById(run.getJobId());
        if (jobOpt.isEmpty()) {
            return Optional.empty();
        }
        var job = jobOpt.get();
        return Optional.of(new AdminBackfillStatusResponse(
                run.getJobId(),
                job.getStatus() != null ? job.getStatus().name() : null,
                job.getProgressCurrent(),
                job.getProgressTotal(),
                run.getProcessed(),
                run.getUpdated(),
                run.getDuplicates(),
                run.getLastCursor(),
                run.isDryRun(),
                run.getAfterId(),
                run.getUntilId(),
                run.getBatchSize(),
                run.getStartedAt() != null ? run.getStartedAt().toString() : null,
                run.getFinishedAt() != null ? run.getFinishedAt().toString() : null,
                firstNonBlank(run.getLastError(), job.getLastError()).orElse(null)
        ));
    }

    public AdminMniMigrationEnqueueResponse enqueueMniMigrationItens(AdminMniMigrationEnqueueRequest request) {
        List<Long> ids = request.itens().stream()
                .map(this::enqueueOne)
                .toList();
        return new AdminMniMigrationEnqueueResponse(ids);
    }

    private Long enqueueOne(AdminMniMigrationItemRequest item) {
        return mniMigrationBatchService.enfileirar(item.tribunalOrigem(), item.motivo(), item.xml());
    }

    public AdminBackfillKickoffResponse kickoffMniMigration(AdminBackfillMniMigrationRequest request,
                                                             String idempotencyKey,
                                                             String clientRequestId) {
        String owner = safeOwner();
        String dedupeKey = firstNonBlank(idempotencyKey, clientRequestId)
                .orElseGet(() -> buildMniMigrationKey(request));
        String inboxKey = request.inboxKey() != null && !request.inboxKey().isBlank()
                ? request.inboxKey().trim()
                : "mni:migracao:lote";
        int priority = request.priority() != null ? Math.max(0, request.priority()) : 0;
        int maxAttempts = request.maxAttempts() != null ? Math.max(1, request.maxAttempts()) : 3;
        int batchSize = request.batchSize() != null ? request.batchSize() : 50;
        MniBatchMigrationJobHandler.Input input = new MniBatchMigrationJobHandler.Input(
                batchSize,
                request.afterId(),
                request.untilId()
        );
        JsonNode inputNode = objectMapper.valueToTree(input);
        JobCommandService.JobCreateResult result = jobCommandService.createIdempotent(
                JobType.MNI_BATCH_MIGRATION,
                inboxKey,
                owner,
                dedupeKey,
                inputNode,
                priority,
                maxAttempts
        );
        backfillRunService.upsertKickoff(
                result.jobId(),
                JobType.MNI_BATCH_MIGRATION.name(),
                inboxKey,
                owner,
                batchSize,
                false,
                request.afterId() != null ? Math.max(0L, request.afterId()) : 0L,
                request.untilId()
        );
        return new AdminBackfillKickoffResponse(result.jobId(), JobStatus.PENDING.name(), result.replay(), result.inProgress());
    }

    public Optional<AdminBackfillStatusResponse> mniMigrationStatus(UUID jobId, String inboxKey) {
        String resolvedInboxKey = inboxKey != null && !inboxKey.isBlank()
                ? inboxKey.trim()
                : "mni:migracao:lote";
        var runOpt = jobId != null
                ? backfillRunService.findById(jobId)
                : backfillRunService.findLatest(JobType.MNI_BATCH_MIGRATION.name(), resolvedInboxKey);
        if (runOpt.isEmpty()) {
            return Optional.empty();
        }
        var run = runOpt.get();
        var jobOpt = jobRepository.findById(run.getJobId());
        if (jobOpt.isEmpty()) {
            return Optional.empty();
        }
        var job = jobOpt.get();
        return Optional.of(new AdminBackfillStatusResponse(
                run.getJobId(),
                job.getStatus() != null ? job.getStatus().name() : null,
                job.getProgressCurrent(),
                job.getProgressTotal(),
                run.getProcessed(),
                run.getUpdated(),
                run.getDuplicates(),
                run.getLastCursor(),
                run.isDryRun(),
                run.getAfterId(),
                run.getUntilId(),
                run.getBatchSize(),
                run.getStartedAt() != null ? run.getStartedAt().toString() : null,
                run.getFinishedAt() != null ? run.getFinishedAt().toString() : null,
                firstNonBlank(run.getLastError(), job.getLastError()).orElse(null)
        ));
    }

    public List<AdminMniMigrationFailedItemDto> mniMigrationFalhas() {
        return mniMigrationBatchService.listarFalhas().stream()
                .map(AdminMniMigrationFailedItemDto::from)
                .toList();
    }

    private static String buildMniMigrationKey(AdminBackfillMniMigrationRequest request) {
        long after = request.afterId() != null ? Math.max(0L, request.afterId()) : 0L;
        String until = request.untilId() != null ? String.valueOf(request.untilId()) : "null";
        int batch = request.batchSize() != null ? request.batchSize() : 50;
        return "MNI_BATCH_MIGRATION:" + after + ':' + until + ':' + batch;
    }

    private String safeOwner() {
        try {
            long id = currentUserService.currentUserIdOrZero();
            return id > 0 ? String.valueOf(id) : "anonymous";
        } catch (Exception ex) {
            return "anonymous";
        }
    }

    private static Optional<String> firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return Optional.of(a);
        }
        if (b != null && !b.isBlank()) {
            return Optional.of(b);
        }
        return Optional.empty();
    }

    private static String buildCanonicalKey(AdminBackfillCanonicalizeSensitiveRequest request) {
        long after = request.afterId() != null ? Math.max(0L, request.afterId()) : 0L;
        String until = request.untilId() != null ? String.valueOf(request.untilId()) : "null";
        boolean dry = request.dryRun() != null && request.dryRun();
        int batch = request.batchSize() != null ? request.batchSize() : 500;
        return "ADV_CLIENTE_CANONICALIZE_SENSITIVE:" + after + ':' + until + ':' + dry + ':' + batch;
    }
}
