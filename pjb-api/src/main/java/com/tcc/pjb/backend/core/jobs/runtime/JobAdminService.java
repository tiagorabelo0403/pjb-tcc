package com.tcc.pjb.backend.core.jobs.runtime;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.jobs.domain.JobStatus;
import com.tcc.pjb.backend.core.jobs.persistence.entity.Job;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;

@Service
public class JobAdminService {

    private final JobRepository repo;

    public JobAdminService(JobRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Transactional
    public Job require(UUID id) {
        Job j = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("job não encontrado: " + id));
        return j;
    }

    @Transactional
    public Job pause(UUID id, String reason) {
        Job j = require(id);
        j.pause(reason == null ? "manual" : reason);
        return repo.save(j);
    }

    @Transactional
    public Job resume(UUID id) {
        Job j = require(id);
        j.resume();
        return repo.save(j);
    }

    @Transactional
    public Job forceRetry(UUID id) {
        Job j = require(id);
        j.forceRetry();
        return repo.save(j);
    }

    @Transactional
    public void markDead(UUID id, String err) {
        Job j = require(id);
        j.fail(err, Instant.now());
        if (j.getStatus() != JobStatus.DEAD) {
            j.forceRetry();
        }
        repo.save(j);
    }
}
