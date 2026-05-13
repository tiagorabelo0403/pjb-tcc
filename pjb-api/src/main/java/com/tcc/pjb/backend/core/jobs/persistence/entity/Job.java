package com.tcc.pjb.backend.core.jobs.persistence.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.tcc.pjb.backend.core.jobs.domain.JobStatus;
import com.tcc.pjb.backend.core.jobs.domain.JobType;

@Entity
@Table(
        name = "tb_job",
        indexes = {
                @Index(name = "ix_job_status_next", columnList = "status,next_retry_at"),
                @Index(name = "ix_job_inbox_status", columnList = "inbox_key,status"),
                @Index(name = "ix_job_owner_created", columnList = "owner_user_id,created_at"),
                @Index(name = "ix_job_claim", columnList = "status,next_retry_at,priority,created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_job_type_idem", columnNames = {"type", "idempotency_key"})
        }
)
public class Job {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 64, nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private JobStatus status;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "inbox_key", length = 240)
    private String inboxKey;

    @Column(name = "owner_user_id", length = 120)
    private String ownerUserId;

    @Column(name = "idempotency_key", length = 180)
    private String idempotencyKey;

    @Lob
    @Column(name = "input_json", columnDefinition = "text")
    private String inputJson;

    @Column(name = "progress_current", nullable = false)
    private long progressCurrent;

    @Column(name = "progress_total", nullable = false)
    private long progressTotal;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Lob
    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "locked_by", length = 120)
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "pause_reason", length = 240)
    private String pauseReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Job() {
    }

    public Job(UUID id,
               JobType type,
               int priority,
               String inboxKey,
               String ownerUserId,
               String idempotencyKey,
               String inputJson,
               int maxAttempts) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.priority = priority;
        this.status = JobStatus.PENDING;
        this.inboxKey = inboxKey;
        this.ownerUserId = ownerUserId;
        this.idempotencyKey = idempotencyKey;
        this.inputJson = inputJson;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.attempts = 0;
        this.progressCurrent = 0;
        this.progressTotal = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public JobType getType() {
        return type;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getPriority() {
        return priority;
    }

    public String getInboxKey() {
        return inboxKey;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getInputJson() {
        return inputJson;
    }

    public long getProgressCurrent() {
        return progressCurrent;
    }

    public long getProgressTotal() {
        return progressTotal;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markRunning(String locker) {
        this.status = JobStatus.RUNNING;
        this.lockedBy = locker;
        this.lockedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void heartbeat() {
        this.lockedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void progress(long current, long total) {
        this.progressCurrent = Math.max(0, current);
        this.progressTotal = Math.max(0, total);
        this.updatedAt = Instant.now();
    }

    public void succeed() {
        this.status = JobStatus.SUCCEEDED;
        this.lockedBy = null;
        this.lockedAt = null;
        this.nextRetryAt = null;
        this.lastError = null;
        this.pausedAt = null;
        this.pauseReason = null;
        this.updatedAt = Instant.now();
    }

    public void pause(String reason) {
        this.status = JobStatus.PAUSED;
        this.lockedBy = null;
        this.lockedAt = null;
        this.pausedAt = Instant.now();
        this.pauseReason = reason;
        this.updatedAt = Instant.now();
    }

    public void resume() {
        if (this.status == JobStatus.PAUSED) {
            this.status = JobStatus.PENDING;
            this.pausedAt = null;
            this.pauseReason = null;
            this.nextRetryAt = null;
            this.updatedAt = Instant.now();
        }
    }

    public void fail(String error, Instant nextRetryAt) {
        this.attempts++;
        this.lastError = error;
        this.lockedBy = null;
        this.lockedAt = null;
        this.updatedAt = Instant.now();

        if (this.attempts >= this.maxAttempts) {
            this.status = JobStatus.DEAD;
            this.nextRetryAt = null;
        } else {
            this.status = JobStatus.FAILED;
            this.nextRetryAt = nextRetryAt;
        }
    }

    public void forceRetry() {
        this.status = JobStatus.PENDING;
        this.nextRetryAt = null;
        this.lockedBy = null;
        this.lockedAt = null;
        this.updatedAt = Instant.now();
    }
}
