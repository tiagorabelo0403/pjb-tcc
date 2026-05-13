package com.tcc.pjb.backend.ai.common.deeprun;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeepRunJob {

    private final UUID id;
    private final DeepRunJobType type;
    private final Instant createdAt;
    private Instant updatedAt;
    private DeepRunStatus status;
    private DeepRunBudget budget;
    private final List<DeepRunCheckpoint> checkpoints = new ArrayList<>();
    private String resultRef;
    private String lastError;

    public DeepRunJob(DeepRunJobType type, DeepRunBudget budget) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.status = DeepRunStatus.CREATED;
        this.budget = budget != null ? budget : DeepRunBudget.default48h();
    }

    public UUID getId() { return id; }
    public DeepRunJobType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public DeepRunStatus getStatus() { return status; }
    public DeepRunBudget getBudget() { return budget; }
    public List<DeepRunCheckpoint> getCheckpoints() { return List.copyOf(checkpoints); }
    public String getResultRef() { return resultRef; }
    public String getLastError() { return lastError; }

    public void setStatus(DeepRunStatus status) {
        this.status = status;
        touch();
    }

    public void setBudget(DeepRunBudget budget) {
        this.budget = budget;
        touch();
    }

    public void addCheckpoint(DeepRunCheckpoint checkpoint) {
        if (checkpoint != null) {
            this.checkpoints.add(checkpoint);
            this.status = DeepRunStatus.CHECKPOINTED;
            touch();
        }
    }

    public void complete(String resultRef) {
        this.resultRef = resultRef;
        this.status = DeepRunStatus.COMPLETED;
        touch();
    }

    public void fail(String error) {
        this.lastError = error;
        this.status = DeepRunStatus.FAILED;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
