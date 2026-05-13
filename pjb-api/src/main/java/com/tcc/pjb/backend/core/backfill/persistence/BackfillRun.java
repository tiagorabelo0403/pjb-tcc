package com.tcc.pjb.backend.core.backfill.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pjb_backfill_run", indexes = {
        @Index(name = "ix_backfill_type_created", columnList = "type, created_at"),
        @Index(name = "ix_backfill_inbox_created", columnList = "inbox_key, created_at")
})
public class BackfillRun {

    @Id
    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "type", nullable = false, length = 80)
    private String type;

    @Column(name = "inbox_key", length = 240)
    private String inboxKey;

    @Column(name = "requested_by", length = 120)
    private String requestedBy;

    @Column(name = "batch_size", nullable = false)
    private int batchSize;

    @Column(name = "dry_run", nullable = false)
    private boolean dryRun;

    @Column(name = "after_id", nullable = false)
    private long afterId;

    @Column(name = "until_id")
    private Long untilId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "processed", nullable = false)
    private long processed;

    @Column(name = "updated", nullable = false)
    private long updated;

    @Column(name = "duplicates", nullable = false)
    private long duplicates;

    @Column(name = "last_cursor", nullable = false)
    private long lastCursor;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BackfillRun() {
    }

    public BackfillRun(UUID jobId,
                       String type,
                       String inboxKey,
                       String requestedBy,
                       int batchSize,
                       boolean dryRun,
                       long afterId,
                       Long untilId) {
        this.jobId = Objects.requireNonNull(jobId);
        this.type = Objects.requireNonNull(type);
        this.inboxKey = inboxKey;
        this.requestedBy = requestedBy;
        this.batchSize = Math.max(1, batchSize);
        this.dryRun = dryRun;
        this.afterId = Math.max(0L, afterId);
        this.untilId = untilId;
        this.processed = 0L;
        this.updated = 0L;
        this.duplicates = 0L;
        this.lastCursor = this.afterId;
    }

    public void markRunningIfNeeded() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
        this.finishedAt = null;
        this.lastError = null;
        touch();
    }

    public void addBatch(long processed, long updated, long duplicates, long lastCursor) {
        this.processed = safeAdd(this.processed, processed);
        this.updated = safeAdd(this.updated, updated);
        this.duplicates = safeAdd(this.duplicates, duplicates);
        if (lastCursor > this.lastCursor) {
            this.lastCursor = lastCursor;
        }
        touch();
    }

    public void markSucceeded() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
        this.finishedAt = Instant.now();
        touch();
    }

    public void markFailed(String error) {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
        this.finishedAt = Instant.now();
        this.lastError = truncate(error, 6000);
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static long safeAdd(long a, long b) {
        if (b <= 0) return a;
        long r = a + b;
        if (r < a) return Long.MAX_VALUE;
        return r;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        if (updatedAt == null) updatedAt = Instant.now();
    }
}
