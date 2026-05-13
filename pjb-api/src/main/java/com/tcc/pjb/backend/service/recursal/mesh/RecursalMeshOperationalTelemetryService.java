package com.tcc.pjb.backend.service.recursal.mesh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardBucket;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshIndexDriftReport;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;

@Service
@ConditionalOnClass(MeterRegistry.class)
public class RecursalMeshOperationalTelemetryService {

    private final MeterRegistry meterRegistry;
    private final AtomicLong projectionCount = new AtomicLong();
    private final AtomicLong indexCount = new AtomicLong();
    private final AtomicInteger sampled = new AtomicInteger();
    private final AtomicInteger missing = new AtomicInteger();
    private final AtomicInteger outdated = new AtomicInteger();
    private final AtomicInteger divergentState = new AtomicInteger();
    private final AtomicInteger divergentRevision = new AtomicInteger();
    private final AtomicInteger reindexProcessed = new AtomicInteger();
    private final AtomicInteger reindexIndexed = new AtomicInteger();
    private final AtomicInteger reindexSkipped = new AtomicInteger();
    private final AtomicInteger reindexBatches = new AtomicInteger();
    private final MultiGauge stuckStateGauge;
    private final MultiGauge stuckTribunalGauge;
    private final MultiGauge stuckAuthorityGauge;
    private final MultiGauge notificationFailureGauge;
    private final MultiGauge retryExhaustedGauge;
    private static final int MAX_CHECKPOINT_TAGS = 64;
    private static final int MAX_CHANNEL_TAGS = 64;
    private static final int MAX_TARGET_TAGS = 96;
    private static final int MAX_OPERATION_TAGS = 48;
    private static final int MAX_BUCKET_TAGS = 96;
    private static final int MAX_NOTIFICATION_FAILURE_BUCKETS = 64;
    private static final int MAX_RETRY_EXHAUSTED_BUCKETS = 128;
    private static final String OTHER_BUCKET = "other";

    private final Map<String, AtomicLong> notificationFailuresByChannel = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> retryExhaustedByTarget = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> checkpointTags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> channelTags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> targetTags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> operationTags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> bucketTags = new ConcurrentHashMap<>();

    public RecursalMeshOperationalTelemetryService(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.stuckStateGauge = MultiGauge.builder("pjb.recursal.mesh.stuck.state")
                .description("Quantidade recursal por estado em gargalo operacional")
                .register(meterRegistry);
        this.stuckTribunalGauge = MultiGauge.builder("pjb.recursal.mesh.stuck.tribunal")
                .description("Quantidade recursal em gargalo operacional por tribunal")
                .register(meterRegistry);
        this.stuckAuthorityGauge = MultiGauge.builder("pjb.recursal.mesh.stuck.authority")
                .description("Quantidade recursal em gargalo operacional por autoridade atual")
                .register(meterRegistry);
        this.notificationFailureGauge = MultiGauge.builder("pjb.recursal.mesh.notification.failure.channel")
                .description("Falhas acumuladas de notificação recursal por canal")
                .register(meterRegistry);
        this.retryExhaustedGauge = MultiGauge.builder("pjb.recursal.mesh.retry.exhausted.target")
                .description("Retentativas exauridas no recursal por alvo operacional")
                .register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.index.drift.projection.count", projectionCount, AtomicLong::get).register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.index.drift.index.count", indexCount, AtomicLong::get).register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.index.drift.sampled", sampled, AtomicInteger::get).register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.index.drift.missing", missing, AtomicInteger::get).register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.index.drift.outdated", outdated, AtomicInteger::get).register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.index.drift.divergent.state", divergentState, AtomicInteger::get).register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.index.drift.divergent.revision", divergentRevision, AtomicInteger::get).register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.reindex.last.processed", reindexProcessed, AtomicInteger::get).register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.reindex.last.indexed", reindexIndexed, AtomicInteger::get).register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.reindex.last.skipped", reindexSkipped, AtomicInteger::get).register(meterRegistry);
        Gauge.builder("pjb.recursal.mesh.reindex.last.batches", reindexBatches, AtomicInteger::get).register(meterRegistry);
    }

    public void recordReindexStarted(String checkpointKey) {
        meterRegistry.counter("pjb.recursal.mesh.reindex.started", "checkpoint", checkpointTag(checkpointKey)).increment();
    }

    public void recordReindexBatch(String checkpointKey, int processedDelta, int indexedDelta, int skippedDelta) {
        counter("pjb.recursal.mesh.reindex.batch.processed", checkpointKey).increment(processedDelta);
        counter("pjb.recursal.mesh.reindex.batch.indexed", checkpointKey).increment(indexedDelta);
        counter("pjb.recursal.mesh.reindex.batch.skipped", checkpointKey).increment(skippedDelta);
        reindexProcessed.addAndGet(Math.max(0, processedDelta));
        reindexIndexed.addAndGet(Math.max(0, indexedDelta));
        reindexSkipped.addAndGet(Math.max(0, skippedDelta));
        reindexBatches.incrementAndGet();
    }

    public void recordReindexCompleted(String checkpointKey, int processed, int indexed, int skipped, int batches) {
        counter("pjb.recursal.mesh.reindex.completed", checkpointKey).increment();
        reindexProcessed.set(Math.max(0, processed));
        reindexIndexed.set(Math.max(0, indexed));
        reindexSkipped.set(Math.max(0, skipped));
        reindexBatches.set(Math.max(0, batches));
    }

    public void recordReindexFailed(String checkpointKey) {
        counter("pjb.recursal.mesh.reindex.failed", checkpointKey).increment();
    }

    public void recordReindexLockContention(String checkpointKey) {
        counter("pjb.recursal.mesh.reindex.lock.contended", checkpointKey).increment();
    }

    public void recordNotificationDelivery(String channel, boolean success) {
        String normalizedChannel = channelTag(channel);
        meterRegistry.counter(
                "pjb.recursal.mesh.notification.delivery",
                "channel", normalizedChannel,
                "result", success ? "success" : "failure"
        ).increment();
        if (!success) {
            incrementBounded(notificationFailuresByChannel, normalizedChannel, MAX_NOTIFICATION_FAILURE_BUCKETS);
            refreshOperationalGauges();
        }
    }

    public void recordRetryAttempt(String operation, String target, int nextAttempt) {
        meterRegistry.counter(
                "pjb.recursal.mesh.retry.attempt",
                "operation", operationTag(operation),
                "target", targetTag(target),
                "attempt", Integer.toString(Math.max(2, nextAttempt))
        ).increment();
    }

    public void recordRetrySuccess(String operation, String target, int attemptsUsed) {
        meterRegistry.counter(
                "pjb.recursal.mesh.retry.success",
                "operation", operationTag(operation),
                "target", targetTag(target),
                "attempts", Integer.toString(Math.max(1, attemptsUsed))
        ).increment();
    }

    public void recordRetryExhausted(String operation, String target) {
        String normalizedOperation = operationTag(operation);
        String normalizedTargetTag = targetTag(target);
        String normalizedTarget = OTHER_BUCKET.equals(normalizedOperation) || OTHER_BUCKET.equals(normalizedTargetTag)
                ? OTHER_BUCKET
                : normalizedOperation + ':' + normalizedTargetTag;
        meterRegistry.counter(
                "pjb.recursal.mesh.retry.exhausted",
                "operation", normalizedOperation,
                "target", normalizedTargetTag
        ).increment();
        incrementBounded(retryExhaustedByTarget, normalizedTarget, MAX_RETRY_EXHAUSTED_BUCKETS);
        refreshOperationalGauges();
    }

    public void updateDrift(RecursalMeshIndexDriftReport report) {
        if (report == null) {
            return;
        }
        projectionCount.set(report.projectionCount());
        indexCount.set(report.indexCount());
        sampled.set(report.sampled());
        missing.set(report.missingInIndex());
        outdated.set(report.outdatedInIndex());
        divergentState.set(report.divergentState());
        divergentRevision.set(report.divergentRevision());
        meterRegistry.counter("pjb.recursal.mesh.index.drift.assessed", "severity", bucketTag(report.severity())).increment();
    }

    public void updateDashboard(RecursalMeshDashboardResponse response) {
        if (response == null) {
            return;
        }
        stuckStateGauge.register(rows(response.gargalosPorEstado(), "state", response.source()), true);
        stuckTribunalGauge.register(rows(response.gargalosPorTribunal(), "tribunal", response.source()), true);
        stuckAuthorityGauge.register(rows(response.gargalosPorAutoridadeAtual(), "authority", response.source()), true);
        refreshOperationalGauges();
    }

    public List<RecursalMeshDashboardBucket> notificationFailureBuckets(int limit) {
        return fromCounterMap(notificationFailuresByChannel, Math.max(1, limit));
    }

    public List<RecursalMeshDashboardBucket> retryExhaustedBuckets(int limit) {
        return fromCounterMap(retryExhaustedByTarget, Math.max(1, limit));
    }

    private void refreshOperationalGauges() {
        notificationFailureGauge.register(rows(fromCounterMap(notificationFailuresByChannel, 20), "channel", "telemetry"), true);
        retryExhaustedGauge.register(rows(fromCounterMap(retryExhaustedByTarget, 20), "target", "telemetry"), true);
    }

    private List<MultiGauge.Row<?>> rows(List<RecursalMeshDashboardBucket> buckets, String keyName, String source) {
        List<MultiGauge.Row<?>> rows = new ArrayList<>();
        for (RecursalMeshDashboardBucket bucket : buckets == null ? List.<RecursalMeshDashboardBucket>of() : buckets) {
            if (bucket == null) {
                continue;
            }
            rows.add(MultiGauge.Row.of(Tags.of(keyName, bucketTag(bucket.key()), "source", bucketTag(source)), bucket.total()));
        }
        return rows;
    }

    private void incrementBounded(Map<String, AtomicLong> source, String key, int maxBuckets) {
        AtomicLong existing = source.get(key);
        if (existing != null) {
            existing.incrementAndGet();
            return;
        }
        int uniqueCapacity = Math.max(1, maxBuckets - 1);
        String effectiveKey = source.containsKey(OTHER_BUCKET) || source.size() >= uniqueCapacity ? OTHER_BUCKET : key;
        source.computeIfAbsent(effectiveKey, ignored -> new AtomicLong()).incrementAndGet();
    }


    private List<RecursalMeshDashboardBucket> fromCounterMap(Map<String, AtomicLong> source, int limit) {
        return source.entrySet().stream()
                .map(entry -> new RecursalMeshDashboardBucket(entry.getKey(), entry.getValue().get()))
                .sorted(Comparator.comparingLong(RecursalMeshDashboardBucket::total).reversed().thenComparing(RecursalMeshDashboardBucket::key))
                .limit(limit)
                .toList();
    }

    private Counter counter(String name, String checkpointKey) {
        return meterRegistry.counter(name, "checkpoint", checkpointTag(checkpointKey));
    }

    private String checkpointTag(String value) {
        return boundedTag(checkpointTags, normalizeTag(value), MAX_CHECKPOINT_TAGS);
    }

    private String channelTag(String value) {
        return boundedTag(channelTags, normalizeTag(value), MAX_CHANNEL_TAGS);
    }

    private String targetTag(String value) {
        return boundedTag(targetTags, normalizeTag(value), MAX_TARGET_TAGS);
    }

    private String operationTag(String value) {
        return boundedTag(operationTags, normalizeTag(value), MAX_OPERATION_TAGS);
    }

    private String bucketTag(String value) {
        return boundedTag(bucketTags, normalizeTag(value), MAX_BUCKET_TAGS);
    }

    private static String boundedTag(ConcurrentHashMap<String, Boolean> seen, String value, int max) {
        if (seen.containsKey(value)) {
            return value;
        }
        if (seen.size() >= max) {
            return "other";
        }
        seen.putIfAbsent(value, Boolean.TRUE);
        return seen.size() > max ? "other" : value;
    }

    private static String normalizeTag(String value) {
        if (value == null || value.isBlank()) {
            return "default";
        }
        return value.trim().replace(' ', '-').toLowerCase();
    }
}
