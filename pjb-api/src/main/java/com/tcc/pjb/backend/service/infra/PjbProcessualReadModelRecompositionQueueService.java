package com.tcc.pjb.backend.service.infra;

import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelRecompositionJob;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleRuntimePolicyService;
import com.tcc.pjb.backend.model.repository.ProcessualReadModelRecompositionJobRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class PjbProcessualReadModelRecompositionQueueService {

    private static final List<String> OPEN_STATUSES = List.of("PENDENTE", "EM_PROCESSAMENTO");
    private final ProcessualReadModelRecompositionJobRepository repository;
    private final PjbDataSourceRoutingProperties properties;
    private final JudicialScaleRuntimePolicyService runtimePolicyService;

    public PjbProcessualReadModelRecompositionQueueService(ProcessualReadModelRecompositionJobRepository repository,
                                                           PjbDataSourceRoutingProperties properties,
                                                           JudicialScaleRuntimePolicyService runtimePolicyService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.runtimePolicyService = Objects.requireNonNull(runtimePolicyService, "runtimePolicyService");
    }

    @Transactional
    public JobView enqueue(String domain,
                           String tribunalCode,
                           String ramoCode,
                           String scopeKey,
                           String requestedBy,
                           String reason) {
        String domainKey = normalizeRequired(domain, "domain");
        String tribunalKey = normalizeNullable(tribunalCode);
        String ramoKey = normalizeNullable(ramoCode);
        String scopeKeyNormalized = normalizeNullable(scopeKey);
        for (ProcessualReadModelRecompositionJob current : repository.findTop50ByStatusInOrderByCreatedAtDesc(OPEN_STATUSES)) {
            if (current == null) {
                continue;
            }
            if (sameScope(current, domainKey, tribunalKey, ramoKey, scopeKeyNormalized)) {
                return toView(current);
            }
        }
        ProcessualReadModelRecompositionJob job = new ProcessualReadModelRecompositionJob();
        job.setDomain(domainKey);
        job.setTribunalCode(tribunalKey);
        job.setRamoCode(ramoKey);
        job.setScopeKey(scopeKeyNormalized);
        job.setStatus("PENDENTE");
        job.setRequestedBy(firstNonBlank(normalizeNullable(requestedBy), "SYSTEM"));
        job.setReason(trimToNull(reason));
        job.setAttemptCount(0);
        job.setNotBeforeAt(Instant.now());
        return toView(repository.save(job));
    }

    @PjbTransactionalBudget(operation = "infra.read-model-recomposition-queue.claim-batch", maxMillis = 5000)
    @Transactional
    public List<ProcessualReadModelRecompositionJob> claimBatch() {
        if (!properties.getProcessualReadModels().isPersistenceEnabled()) {
            return List.of();
        }
        List<ProcessualReadModelRecompositionJob> batch = new java.util.ArrayList<>(repository.findByStatusAndNotBeforeAtLessThanEqualOrderByCreatedAtAsc(
                "PENDENTE",
                Instant.now(),
                PageRequest.of(0, Math.max(1, properties.getProcessualReadModels().getRecompositionBatchSize() * 4))
        ));
        if (batch.isEmpty()) {
            return List.of();
        }
        batch.sort(Comparator
                .comparingDouble((ProcessualReadModelRecompositionJob job) -> runtimePolicyService.priorityWeight(job)).reversed()
                .thenComparing(ProcessualReadModelRecompositionJob::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        int claimLimit = Math.max(1, Math.min(batch.size(), runtimePolicyService.recompositionClaimBatch(batch.get(0))));
        List<ProcessualReadModelRecompositionJob> selected = batch.stream().limit(claimLimit).toList();
        Instant now = Instant.now();
        for (ProcessualReadModelRecompositionJob job : selected) {
            if (job == null) {
                continue;
            }
            job.setStatus("EM_PROCESSAMENTO");
            job.setLastClaimedAt(now);
            job.setAttemptCount((job.getAttemptCount() == null ? 0 : Math.max(0, job.getAttemptCount())) + 1);
        }
        return repository.saveAll(selected);
    }

    @Transactional
    public JobView complete(Long jobId, String reason) {
        ProcessualReadModelRecompositionJob job = repository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job de recomposicao nao encontrado."));
        job.setStatus("CONCLUIDO");
        job.setLastCompletedAt(Instant.now());
        job.setLastError(null);
        if (trimToNull(reason) != null) {
            job.setReason(reason.trim());
        }
        return toView(repository.save(job));
    }

    @Transactional
    public JobView fail(Long jobId, String error) {
        ProcessualReadModelRecompositionJob job = repository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job de recomposicao nao encontrado."));
        int attempts = job.getAttemptCount() == null ? 0 : Math.max(0, job.getAttemptCount());
        int maxAttempts = Math.max(1, properties.getProcessualReadModels().getRecompositionMaxAttempts());
        job.setLastError(trimToNull(error));
        if (attempts >= maxAttempts) {
            job.setStatus("FALHO");
        } else {
            job.setStatus("PENDENTE");
            job.setNotBeforeAt(Instant.now().plus(properties.getProcessualReadModels().getRecompositionRetryDelay()));
        }
        return toView(repository.save(job));
    }

    @Transactional(readOnly = true)
    public QueueSnapshot snapshot() {
        List<JobView> jobs = repository.findTop20ByOrderByUpdatedAtDesc().stream().map(this::toView).toList();
        return new QueueSnapshot(
                repository.countByStatusIgnoreCase("PENDENTE"),
                repository.countByStatusIgnoreCase("EM_PROCESSAMENTO"),
                repository.countByStatusIgnoreCase("FALHO"),
                repository.countByStatusIgnoreCase("CONCLUIDO"),
                jobs
        );
    }

    private boolean sameScope(ProcessualReadModelRecompositionJob job,
                              String domain,
                              String tribunalCode,
                              String ramoCode,
                              String scopeKey) {
        return equalsNormalized(job.getDomain(), domain)
                && equalsNormalized(job.getTribunalCode(), tribunalCode)
                && equalsNormalized(job.getRamoCode(), ramoCode)
                && equalsNormalized(job.getScopeKey(), scopeKey);
    }

    private boolean equalsNormalized(String left, String right) {
        return Objects.equals(normalizeNullable(left), normalizeNullable(right));
    }

    private JobView toView(ProcessualReadModelRecompositionJob job) {
        return new JobView(
                job.getId(),
                job.getDomain(),
                job.getTribunalCode(),
                job.getRamoCode(),
                job.getScopeKey(),
                job.getStatus(),
                job.getRequestedBy(),
                job.getReason(),
                job.getAttemptCount(),
                job.getLastError(),
                job.getNotBeforeAt(),
                job.getLastClaimedAt(),
                job.getLastCompletedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " obrigatorio para recomposicao.");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public record QueueSnapshot(
            long pending,
            long processing,
            long failed,
            long completed,
            List<JobView> jobs
    ) {
    }

    public record JobView(
            Long id,
            String domain,
            String tribunalCode,
            String ramoCode,
            String scopeKey,
            String status,
            String requestedBy,
            String reason,
            Integer attemptCount,
            String lastError,
            Instant notBeforeAt,
            Instant lastClaimedAt,
            Instant lastCompletedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
