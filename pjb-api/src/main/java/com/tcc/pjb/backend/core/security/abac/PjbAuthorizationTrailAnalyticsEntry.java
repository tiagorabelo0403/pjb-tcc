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
@Table(name = "tb_authz_trail_analytics", indexes = {
        @Index(name = "idx_authz_analytics_bucket", columnList = "granularity, bucket_started_at, dimension_type"),
        @Index(name = "idx_authz_analytics_dimension", columnList = "dimension_type, dimension_code, bucket_started_at"),
        @Index(name = "idx_authz_analytics_materialized", columnList = "materialized_at"),
        @Index(name = "idx_authz_analytics_unique_key", columnList = "analytics_key", unique = true)
})
public class PjbAuthorizationTrailAnalyticsEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "granularity", nullable = false, length = 16)
    private String granularity;

    @Column(name = "bucket_started_at", nullable = false)
    private LocalDateTime bucketStartedAt;

    @Column(name = "bucket_ended_at_exclusive", nullable = false)
    private LocalDateTime bucketEndedAtExclusive;

    @Column(name = "dimension_type", nullable = false, length = 64)
    private String dimensionType;

    @Column(name = "dimension_code", nullable = false, length = 160)
    private String dimensionCode;

    @Column(name = "dimension_label", nullable = false, length = 160)
    private String dimensionLabel;

    @Column(name = "total_count", nullable = false)
    private long totalCount;

    @Column(name = "allowed_count", nullable = false)
    private long allowedCount;

    @Column(name = "denied_count", nullable = false)
    private long deniedCount;

    @Column(name = "critico_count", nullable = false)
    private long criticoCount;

    @Column(name = "governance_denied_count", nullable = false)
    private long governanceDeniedCount;

    @Column(name = "step_up_denied_count", nullable = false)
    private long stepUpDeniedCount;

    @Column(name = "unique_request_count", nullable = false)
    private long uniqueRequestCount;

    @Column(name = "unique_actor_count", nullable = false)
    private long uniqueActorCount;

    @Column(name = "first_occurred_at", nullable = false)
    private LocalDateTime firstOccurredAt;

    @Column(name = "last_occurred_at", nullable = false)
    private LocalDateTime lastOccurredAt;

    @Column(name = "materialized_at", nullable = false)
    private LocalDateTime materializedAt;

    @Column(name = "source_event_count", nullable = false)
    private long sourceEventCount;

    @Column(name = "analytics_key", nullable = false, length = 280, unique = true)
    private String analyticsKey;

    public static PjbAuthorizationTrailAnalyticsEntry of(PjbAuthorizationTrailTemporalGranularity granularity,
                                                         Instant bucketStartedAt,
                                                         Instant bucketEndedAtExclusive,
                                                         PjbAuthorizationTrailAnalyticsDimensionType dimensionType,
                                                         String dimensionCode,
                                                         String dimensionLabel,
                                                         long totalCount,
                                                         long allowedCount,
                                                         long deniedCount,
                                                         long criticoCount,
                                                         long governanceDeniedCount,
                                                         long stepUpDeniedCount,
                                                         long uniqueRequestCount,
                                                         long uniqueActorCount,
                                                         Instant firstOccurredAt,
                                                         Instant lastOccurredAt,
                                                         Instant materializedAt,
                                                         long sourceEventCount) {
        PjbAuthorizationTrailAnalyticsEntry entry = new PjbAuthorizationTrailAnalyticsEntry();
        entry.granularity = granularity == null ? PjbAuthorizationTrailTemporalGranularity.DAY.name() : granularity.name();
        entry.bucketStartedAt = toLocalDateTime(bucketStartedAt);
        entry.bucketEndedAtExclusive = toLocalDateTime(bucketEndedAtExclusive);
        entry.dimensionType = dimensionType == null ? PjbAuthorizationTrailAnalyticsDimensionType.OVERVIEW.name() : dimensionType.name();
        entry.dimensionCode = normalizeValue(dimensionCode, "ALL");
        entry.dimensionLabel = normalizeValue(dimensionLabel, entry.dimensionCode);
        entry.totalCount = Math.max(0L, totalCount);
        entry.allowedCount = Math.max(0L, allowedCount);
        entry.deniedCount = Math.max(0L, deniedCount);
        entry.criticoCount = Math.max(0L, criticoCount);
        entry.governanceDeniedCount = Math.max(0L, governanceDeniedCount);
        entry.stepUpDeniedCount = Math.max(0L, stepUpDeniedCount);
        entry.uniqueRequestCount = Math.max(0L, uniqueRequestCount);
        entry.uniqueActorCount = Math.max(0L, uniqueActorCount);
        entry.firstOccurredAt = toLocalDateTime(firstOccurredAt);
        entry.lastOccurredAt = toLocalDateTime(lastOccurredAt);
        entry.materializedAt = toLocalDateTime(materializedAt == null ? Instant.now() : materializedAt);
        entry.sourceEventCount = Math.max(0L, sourceEventCount);
        entry.analyticsKey = entry.granularity + '|' + entry.bucketStartedAt + '|' + entry.dimensionType + '|' + entry.dimensionCode;
        return entry;
    }

    public Instant bucketStartedAtInstant() {
        return toInstant(bucketStartedAt);
    }

    public Instant bucketEndedAtExclusiveInstant() {
        return toInstant(bucketEndedAtExclusive);
    }

    public Instant materializedAtInstant() {
        return toInstant(materializedAt);
    }

    @PrePersist
    void prePersist() {
        if (materializedAt == null) {
            materializedAt = LocalDateTime.now(ZoneOffset.UTC);
        }
        if (analyticsKey == null || analyticsKey.isBlank()) {
            analyticsKey = normalizeValue(granularity, "DAY") + '|' + bucketStartedAt + '|' + normalizeValue(dimensionType, "OVERVIEW") + '|' + normalizeValue(dimensionCode, "ALL");
        }
    }

    private static LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC) : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? Instant.EPOCH : value.toInstant(ZoneOffset.UTC);
    }

    private static String normalizeValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
