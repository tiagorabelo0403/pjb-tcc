package com.tcc.pjb.backend.core.backfill.service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.backfill.persistence.BackfillRun;
import com.tcc.pjb.backend.core.backfill.persistence.BackfillRunRepository;

@Service
public class BackfillRunService {

    private final BackfillRunRepository repository;

    public BackfillRunService(BackfillRunRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Transactional
    public BackfillRun upsertKickoff(UUID jobId,
                                    String type,
                                    String inboxKey,
                                    String requestedBy,
                                    int batchSize,
                                    boolean dryRun,
                                    long afterId,
                                    Long untilId) {
        Optional<BackfillRun> existing = repository.findById(jobId);
        if (existing.isPresent()) {
            BackfillRun r = existing.get();
            r.setType(type);
            r.setInboxKey(inboxKey);
            r.setRequestedBy(requestedBy);
            r.setBatchSize(Math.max(1, batchSize));
            r.setDryRun(dryRun);
            r.setAfterId(Math.max(0L, afterId));
            r.setUntilId(untilId);
            return repository.save(r);
        }
        BackfillRun created = new BackfillRun(jobId, type, inboxKey, requestedBy, batchSize, dryRun, afterId, untilId);
        return repository.save(created);
    }

    @Transactional
    public void markRunning(UUID jobId) {
        repository.findById(jobId).ifPresent(r -> {
            r.markRunningIfNeeded();
            repository.save(r);
        });
    }

    @Transactional
    public void recordBatch(UUID jobId, long processed, long updated, long duplicates, long lastCursor) {
        repository.findById(jobId).ifPresent(r -> {
            r.addBatch(processed, updated, duplicates, lastCursor);
            repository.save(r);
        });
    }

    @Transactional
    public void markSucceeded(UUID jobId) {
        repository.findById(jobId).ifPresent(r -> {
            r.markSucceeded();
            repository.save(r);
        });
    }

    @Transactional
    public void markFailed(UUID jobId, String error) {
        repository.findById(jobId).ifPresent(r -> {
            r.markFailed(error);
            repository.save(r);
        });
    }

    @Transactional(readOnly = true)
    public Optional<BackfillRun> findLatest(String type, String inboxKey) {
        return repository.findLatest(type, inboxKey);
    }

    @Transactional(readOnly = true)
    public Optional<BackfillRun> findById(UUID jobId) {
        return repository.findById(jobId);
    }
}
