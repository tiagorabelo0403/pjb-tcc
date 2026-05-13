package com.tcc.pjb.backend.core.jobs.runtime;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.IdempotencyBeginResult;
import com.tcc.pjb.backend.core.idempotency.IdempotencyDecision;
import com.tcc.pjb.backend.core.jobs.domain.JobStatus;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.persistence.entity.Job;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;

@Service
public class JobCommandService {

    public record JobCreateResult(UUID jobId, boolean replay, boolean inProgress) {
    }

    private final JobRepository jobRepository;
    private final ActionIdempotencyService idempotency;
    private final CanonicalJsonHasher hasher;
    private final ObjectMapper mapper;
    private final JobPgNotifyEmitter notifyEmitter;

    public JobCommandService(JobRepository jobRepository,
                             ActionIdempotencyService idempotency,
                             CanonicalJsonHasher hasher,
                             ObjectMapper mapper,
                             JobPgNotifyEmitter notifyEmitter) {
        this.jobRepository = Objects.requireNonNull(jobRepository);
        this.idempotency = Objects.requireNonNull(idempotency);
        this.hasher = Objects.requireNonNull(hasher);
        this.mapper = Objects.requireNonNull(mapper);
        this.notifyEmitter = Objects.requireNonNull(notifyEmitter);
    }

    @Transactional
    public JobCreateResult createIdempotent(JobType type,
                                            String inboxKey,
                                            String ownerUserId,
                                            String idempotencyKey,
                                            Object input,
                                            int priority,
                                            int maxAttempts) {

        Objects.requireNonNull(type);
        Objects.requireNonNull(idempotencyKey);

        String scope = "JOB_CREATE";
        String requestHash = hasher.fingerprint(new RequestEnvelope(type.name(), inboxKey, ownerUserId, idempotencyKey, input, priority, maxAttempts)).sha256();
        IdempotencyBeginResult begin = idempotency.begin(scope, idempotencyKey, requestHash, Duration.ofSeconds(30));

        if (begin.decision() == IdempotencyDecision.REPLAY) {
            UUID id = parseUuid(begin.resourceIdOptional().orElse(null)).orElse(null);
            if (id != null) {
                return new JobCreateResult(id, true, false);
            }
        }

        if (begin.decision() == IdempotencyDecision.IN_PROGRESS) {
            UUID id = parseUuid(begin.resourceIdOptional().orElse(null)).orElseGet(() -> jobRepository.findByTypeAndIdempotencyKey(type, idempotencyKey).map(Job::getId).orElse(null));
            if (id != null) {
                return new JobCreateResult(id, false, true);
            }
        }

        
        UUID jobId = UUID.randomUUID();
        String inputJson = safeJson(input);
        Job job = new Job(jobId, type, priority, inboxKey, ownerUserId, idempotencyKey, inputJson, maxAttempts);
        jobRepository.save(job);
        notifyEmitter.notifyJobCreated(jobId.toString());

        String responseJson = safeJson(new CreateResponse(jobId.toString(), type.name(), JobStatus.PENDING.name()));
        String responseHash = hasher.fingerprint(responseJson).sha256();
        idempotency.complete(scope, idempotencyKey, responseHash, "JOB", jobId.toString(), responseJson);

        return new JobCreateResult(jobId, false, false);
    }

    private String safeJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private record RequestEnvelope(String type, String inboxKey, String ownerUserId, String idempotencyKey, Object input, int priority, int maxAttempts) {
    }

    private record CreateResponse(String jobId, String jobType, String status) {
    }
}
