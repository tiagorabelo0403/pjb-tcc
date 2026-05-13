package com.tcc.pjb.backend.core.jobs.runtime;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.jobs.persistence.entity.Job;
import com.tcc.pjb.backend.core.jobs.persistence.entity.JobItem;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobItemRepository;

public final class JobExecutionContext {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionContext.class);

    private final String instanceId;
    private final Job job;
    private final ObjectMapper mapper;
    private final JobItemRepository jobItemRepository;
    private final JobClaimDao claimDao;

    JobExecutionContext(String instanceId,
                        Job job,
                        ObjectMapper mapper,
                        JobItemRepository jobItemRepository,
                        JobClaimDao claimDao) {
        this.instanceId = Objects.requireNonNull(instanceId);
        this.job = Objects.requireNonNull(job);
        this.mapper = Objects.requireNonNull(mapper);
        this.jobItemRepository = Objects.requireNonNull(jobItemRepository);
        this.claimDao = Objects.requireNonNull(claimDao);
    }

    public String instanceId() {
        return instanceId;
    }

    public UUID jobId() {
        return job.getId();
    }

    public Job job() {
        return job;
    }

    public String inputJson() {
        return job.getInputJson();
    }

    public JsonNode inputAsJson() {
        try {
            String raw = job.getInputJson();
            return raw == null || raw.isBlank() ? mapper.createObjectNode() : mapper.readTree(raw);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    public <T> T inputAs(Class<T> type) {
        try {
            String raw = job.getInputJson();
            if (raw == null || raw.isBlank()) {
                return mapper.convertValue(mapper.createObjectNode(), type);
            }
            return mapper.readValue(raw, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("input_json inválido para job " + job.getId(), e);
        }
    }

    public void progress(long current, long total) {
        job.progress(current, total);
    }

    public void heartbeat() {
        try {
            claimDao.touchLock(job.getId(), instanceId, Instant.now());
        } catch (Exception e) {
            log.warn("job heartbeat falhou: jobId={} err={}", job.getId(), e.getMessage());
        }
        job.heartbeat();
    }

    public List<JobItem> items() {
        return jobItemRepository.findByJobIdOrderByCreatedAtAsc(job.getId());
    }

    public Optional<JobItem> findOrCreateItem(String itemKey, int maxAttempts) {
        Optional<JobItem> existing = jobItemRepository.findByJobIdAndItemKey(job.getId(), itemKey);
        if (existing.isPresent()) {
            return existing;
        }
        JobItem created = new JobItem(UUID.randomUUID(), job, itemKey, maxAttempts);
        return Optional.of(jobItemRepository.save(created));
    }
}
