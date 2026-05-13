package com.tcc.pjb.backend.service.jobs.surface;

import com.tcc.pjb.backend.core.jobs.domain.JobStatus;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.persistence.entity.Job;
import com.tcc.pjb.backend.core.jobs.persistence.entity.JobItem;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobItemRepository;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;
import com.tcc.pjb.backend.core.jobs.runtime.JobAdminService;
import com.tcc.pjb.backend.core.jobs.runtime.JobCommandService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.jobs.JobCreateRequest;
import com.tcc.pjb.backend.model.dto.jobs.JobCreateResponse;
import com.tcc.pjb.backend.model.dto.jobs.JobItemResponse;
import com.tcc.pjb.backend.model.dto.jobs.JobResponse;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class JobsSurfaceFacadeService {

    private final JobCommandService jobCommandService;
    private final JobAdminService adminService;
    private final JobRepository jobRepository;
    private final JobItemRepository jobItemRepository;
    private final CurrentUserService currentUserService;

    public JobsSurfaceFacadeService(JobCommandService jobCommandService,
                                    JobAdminService adminService,
                                    JobRepository jobRepository,
                                    JobItemRepository jobItemRepository,
                                    CurrentUserService currentUserService) {
        this.jobCommandService = Objects.requireNonNull(jobCommandService);
        this.adminService = Objects.requireNonNull(adminService);
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.jobItemRepository = Objects.requireNonNull(jobItemRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    public JobCreateResponse create(JobCreateRequest req, String idempotencyKey, String clientRequestId) {
        String owner = safeOwner();
        String key = firstNonBlank(idempotencyKey, clientRequestId).orElse(UUID.randomUUID().toString());
        JobType type = JobType.valueOf(req.type().trim().toUpperCase(Locale.ROOT));
        JobCommandService.JobCreateResult result = jobCommandService.createIdempotent(
                type,
                nz(req.inboxKey()),
                owner,
                key,
                req.input(),
                req.priority() != null ? req.priority() : 0,
                req.maxAttempts() != null ? req.maxAttempts() : 10
        );
        return new JobCreateResponse(result.jobId(), JobStatus.PENDING.name(), result.replay(), result.inProgress());
    }

    public JobResponse get(UUID id) {
        return toResponse(adminService.require(id));
    }

    public Page<JobResponse> list(String inboxKey, JobStatus status, int page, int size) {
        Page<Job> payload = jobRepository.list(inboxKey, status, PageRequest.of(Math.max(0, page), clamp(size, 1, 200), Sort.by(Sort.Direction.DESC, "createdAt")));
        return payload.map(this::toResponse);
    }

    public JobResponse pause(UUID id, String reason) {
        return toResponse(adminService.pause(id, reason));
    }

    public JobResponse resume(UUID id) {
        return toResponse(adminService.resume(id));
    }

    public JobResponse forceRetry(UUID id) {
        return toResponse(adminService.forceRetry(id));
    }

    public List<JobItemResponse> items(UUID id) {
        return jobItemRepository.findByJobIdOrderByCreatedAtAsc(id).stream().map(this::toItemResponse).toList();
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getType() != null ? job.getType().name() : null,
                job.getStatus() != null ? job.getStatus().name() : null,
                job.getPriority(),
                job.getInboxKey(),
                job.getOwnerUserId(),
                job.getIdempotencyKey(),
                job.getProgressCurrent(),
                job.getProgressTotal(),
                job.getAttempts(),
                job.getMaxAttempts(),
                job.getNextRetryAt(),
                safeTrunc(job.getLastError(), 2000),
                job.getLockedBy(),
                job.getLockedAt(),
                job.getPausedAt(),
                job.getPauseReason(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private JobItemResponse toItemResponse(JobItem item) {
        return new JobItemResponse(item.getId(), item.getItemKey(), item.getStatus().name(), item.getAttempts(), item.getMaxAttempts(), safeTrunc(item.getLastError(), 1200), item.getCreatedAt(), item.getUpdatedAt());
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
        if (a != null && !a.isBlank()) return Optional.of(a);
        if (b != null && !b.isBlank()) return Optional.of(b);
        return Optional.empty();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String safeTrunc(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
