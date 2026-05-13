package com.tcc.pjb.backend.core.security.abac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tb_authz_trail_analytics_refresh_queue", indexes = {
        @Index(name = "idx_authz_analytics_refresh_status", columnList = "status, next_visible_at, bucket_started_at"),
        @Index(name = "idx_authz_analytics_refresh_bucket", columnList = "granularity, bucket_started_at"),
        @Index(name = "idx_authz_analytics_refresh_processed", columnList = "last_processed_at"),
        @Index(name = "idx_authz_analytics_refresh_key", columnList = "dedup_key", unique = true)
})
public class PjbAuthorizationTrailAnalyticsRefreshQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "granularity", nullable = false, length = 16)
    private String granularity;

    @Column(name = "bucket_started_at", nullable = false)
    private LocalDateTime bucketStartedAt;

    @Column(name = "bucket_ended_at_exclusive", nullable = false)
    private LocalDateTime bucketEndedAtExclusive;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "requeue_requested", nullable = false)
    private boolean requeueRequested;

    @Column(name = "next_visible_at", nullable = false)
    private LocalDateTime nextVisibleAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "last_enqueued_at", nullable = false)
    private LocalDateTime lastEnqueuedAt;

    @Column(name = "last_processed_at")
    private LocalDateTime lastProcessedAt;

    @Column(name = "last_error", nullable = false, columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "dedup_key", nullable = false, unique = true, length = 160)
    private String dedupKey;

    public static PjbAuthorizationTrailAnalyticsRefreshQueueEntry of(PjbAuthorizationTrailTemporalGranularity granularity,
                                                                     Instant bucketStartedAt,
                                                                     Instant bucketEndedAtExclusive,
                                                                     Instant now) {
        Instant effectiveNow = now == null ? Instant.now() : now;
        Instant effectiveStart = bucketStartedAt == null ? effectiveNow : bucketStartedAt;
        Instant effectiveEnd = bucketEndedAtExclusive == null ? effectiveStart : bucketEndedAtExclusive;
        PjbAuthorizationTrailAnalyticsRefreshQueueEntry entry = new PjbAuthorizationTrailAnalyticsRefreshQueueEntry();
        entry.granularity = granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY.name() : granularity.name();
        entry.bucketStartedAt = toLocalDateTime(effectiveStart);
        entry.bucketEndedAtExclusive = toLocalDateTime(effectiveEnd);
        entry.status = PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PENDING.name();
        entry.attemptCount = 0;
        entry.requeueRequested = false;
        entry.nextVisibleAt = toLocalDateTime(effectiveNow);
        entry.lastEnqueuedAt = toLocalDateTime(effectiveNow);
        entry.lastError = "";
        entry.dedupKey = entry.granularity + '|' + entry.bucketStartedAt;
        return entry;
    }

    public Instant bucketStartedAtInstant() {
        return toInstant(bucketStartedAt);
    }

    public Instant bucketEndedAtExclusiveInstant() {
        return toInstant(bucketEndedAtExclusive);
    }

    public Instant nextVisibleAtInstant() {
        return toInstant(nextVisibleAt);
    }

    public Instant lastProcessedAtInstant() {
        return toInstant(lastProcessedAt);
    }

    public void markEnqueued(Instant now) {
        Instant effectiveNow = now == null ? Instant.now() : now;
        lastEnqueuedAt = toLocalDateTime(effectiveNow);
        if (PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PROCESSING.name().equals(status)) {
            requeueRequested = true;
            return;
        }
        status = PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PENDING.name();
        nextVisibleAt = toLocalDateTime(effectiveNow);
        lockedAt = null;
        lastError = "";
    }

    public void markCompleted(Instant now) {
        Instant effectiveNow = now == null ? Instant.now() : now;
        lastProcessedAt = toLocalDateTime(effectiveNow);
        lockedAt = null;
        lastError = "";
        if (requeueRequested) {
            status = PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PENDING.name();
            requeueRequested = false;
            nextVisibleAt = toLocalDateTime(effectiveNow);
            return;
        }
        status = PjbAuthorizationTrailAnalyticsRefreshQueueStatus.COMPLETED.name();
        nextVisibleAt = toLocalDateTime(effectiveNow);
    }

    public void markFailed(Instant now, String error) {
        Instant effectiveNow = now == null ? Instant.now() : now;
        status = PjbAuthorizationTrailAnalyticsRefreshQueueStatus.FAILED.name();
        lockedAt = null;
        lastProcessedAt = toLocalDateTime(effectiveNow);
        lastError = normalizeError(error);
        nextVisibleAt = toLocalDateTime(effectiveNow.plusSeconds(backoffSeconds()));
    }

    private long backoffSeconds() {
        return Math.min(300L, Math.max(5L, attemptCount * 10L));
    }

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = PjbAuthorizationTrailAnalyticsRefreshQueueStatus.PENDING.name();
        }
        if (nextVisibleAt == null) {
            nextVisibleAt = LocalDateTime.now(ZoneOffset.UTC);
        }
        if (lastEnqueuedAt == null) {
            lastEnqueuedAt = LocalDateTime.now(ZoneOffset.UTC);
        }
        if (lastError == null) {
            lastError = "";
        }
        if (dedupKey == null || dedupKey.isBlank()) {
            dedupKey = normalize(granularity, PjbAuthorizationTrailTemporalGranularity.DAY.name()) + '|' + bucketStartedAt;
        }
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String normalizeError(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replace('\n', ' ').replace('\r', ' ');
        return normalized.length() > 4000 ? normalized.substring(0, 4000) : normalized;
    }

    private static LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
