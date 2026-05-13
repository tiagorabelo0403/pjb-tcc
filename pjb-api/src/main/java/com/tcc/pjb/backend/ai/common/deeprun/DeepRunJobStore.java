package com.tcc.pjb.backend.ai.common.deeprun;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class DeepRunJobStore {

    private static final int MAX_JOBS = 2048;
    private static final int TARGET_JOBS = 1536;
    private static final Duration TERMINAL_TTL = Duration.ofHours(24);
    private static final Duration ACTIVE_TTL = Duration.ofHours(72);

    private final Map<UUID, DeepRunJob> jobs = new ConcurrentHashMap<>();
    private final AtomicLong cleanupClock = new AtomicLong();

    public DeepRunJob create(DeepRunJobType type, DeepRunBudget budget) {
        maybeCleanup();
        DeepRunJob job = new DeepRunJob(type, budget);
        jobs.put(job.getId(), job);
        if (jobs.size() > MAX_JOBS) {
            trimOverflow();
        }
        return job;
    }

    public Optional<DeepRunJob> get(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        maybeCleanup();
        DeepRunJob job = jobs.get(id);
        if (job == null) {
            return Optional.empty();
        }
        if (isExpired(job, Instant.now())) {
            jobs.remove(id, job);
            return Optional.empty();
        }
        return Optional.of(job);
    }

    public List<DeepRunJob> list() {
        maybeCleanup();
        return jobs.values().stream()
                .sorted(Comparator.comparing(DeepRunJob::getUpdatedAt).reversed())
                .toList();
    }

    private void maybeCleanup() {
        long tick = cleanupClock.incrementAndGet();
        if ((tick & 127L) != 0L && jobs.size() <= MAX_JOBS) {
            return;
        }
        Instant now = Instant.now();
        jobs.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        if (jobs.size() > MAX_JOBS) {
            trimOverflow();
        }
    }

    private boolean isExpired(DeepRunJob job, Instant now) {
        if (job == null) {
            return true;
        }
        Duration ttl = isTerminal(job) ? TERMINAL_TTL : ACTIVE_TTL;
        Instant updatedAt = job.getUpdatedAt() == null ? job.getCreatedAt() : job.getUpdatedAt();
        return updatedAt == null || updatedAt.plus(ttl).isBefore(now);
    }

    private boolean isTerminal(DeepRunJob job) {
        if (job == null || job.getStatus() == null) {
            return true;
        }
        return switch (job.getStatus()) {
            case COMPLETED, FAILED, CANCELLED -> true;
            default -> false;
        };
    }

    private void trimOverflow() {
        int overflow = jobs.size() - TARGET_JOBS;
        if (overflow <= 0) {
            return;
        }
        jobs.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().getUpdatedAt() == null ? Instant.EPOCH : entry.getValue().getUpdatedAt()))
                .limit(overflow)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(jobs::remove);
    }
}
