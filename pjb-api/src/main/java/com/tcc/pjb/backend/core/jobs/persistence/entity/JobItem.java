package com.tcc.pjb.backend.core.jobs.persistence.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.tcc.pjb.backend.core.jobs.domain.JobItemStatus;

@Entity
@Table(
        name = "tb_job_item",
        indexes = {
                @Index(name = "ix_job_item_job_status", columnList = "job_id,status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_job_item_job_key", columnNames = {"job_id", "item_key"})
        }
)
public class JobItem {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "item_key", length = 240, nullable = false)
    private String itemKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private JobItemStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Lob
    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobItem() {
    }

    public JobItem(UUID id, Job job, String itemKey, int maxAttempts) {
        this.id = Objects.requireNonNull(id);
        this.job = Objects.requireNonNull(job);
        this.itemKey = Objects.requireNonNull(itemKey);
        this.status = JobItemStatus.PENDING;
        this.attempts = 0;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public String getItemKey() {
        return itemKey;
    }

    public JobItemStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markRunning() {
        this.status = JobItemStatus.RUNNING;
        this.updatedAt = Instant.now();
    }

    public void succeed() {
        this.status = JobItemStatus.SUCCEEDED;
        this.lastError = null;
        this.updatedAt = Instant.now();
    }

    public void fail(String error) {
        this.attempts++;
        this.lastError = error;
        this.updatedAt = Instant.now();
        if (this.attempts >= this.maxAttempts) {
            this.status = JobItemStatus.DEAD;
        } else {
            this.status = JobItemStatus.FAILED;
        }
    }

    public void resetForRetry() {
        if (this.status == JobItemStatus.FAILED || this.status == JobItemStatus.DEAD) {
            this.status = JobItemStatus.PENDING;
            this.updatedAt = Instant.now();
        }
    }
}
