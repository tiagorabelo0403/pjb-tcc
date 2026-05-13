package com.tcc.pjb.backend.platform.security.idempotency;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyAuditView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyDecisionView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyExecutionCommand;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyExecutionResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyHealthSnapshot;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeyHealthView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeyQuery;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeyResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeySnapshot;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayPayload;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayQuery;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyReplayView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyStatusView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyTimelineEntry;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyTimelineResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyWindowQuery;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyWindowResult;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

public class PjbIdempotencyService {
    private static final String PREFIX = "pjb:idem:";
    private static final String PROCESSING = "PROCESSING";
    private static final String LEGACY_OK = "OK";
    private static final String DONE_PREFIX = "DONE|";

    private final StringRedisTemplate redis;
    private final PjbIdempotencyPolicy policy;
    private final AuditLedgerService auditLedger;

    public PjbIdempotencyService(StringRedisTemplate redis,
                                 PjbIdempotencyPolicy policy,
                                 AuditLedgerService auditLedger) {
        this.redis = Objects.requireNonNull(redis);
        this.policy = Objects.requireNonNull(policy);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    public PjbIdempotencyExecutionResult execute(PjbIdempotencyExecutionCommand command) {
        boolean acquired = acquire(command.key());
        return new PjbIdempotencyExecutionResult(command.key(), acquired, status(command.key()));
    }

    public PjbIdempotencyKeyResult query(PjbIdempotencyKeyQuery query) {
        String currentStatus = status(query.key());
        return new PjbIdempotencyKeyResult(query.key(), currentStatus, retryAfterSeconds());
    }

    public PjbIdempotencyKeySnapshot snapshot(String key) {
        return new PjbIdempotencyKeySnapshot(key, status(key), Instant.now());
    }

    public PjbIdempotencyReplayView replayView(String key) {
        String currentStatus = status(key);
        Optional<PjbIdempotencyReplayPayload> payload = loadReplay(key);
        return new PjbIdempotencyReplayView(key, payload.isPresent() || (currentStatus != null && !PROCESSING.equals(currentStatus)), currentStatus);
    }

    public PjbIdempotencyHealthSnapshot healthSnapshot() {
        return new PjbIdempotencyHealthSnapshot(true, retryAfterSeconds(), Instant.now());
    }

    public PjbIdempotencyStatusView statusView(String key) {
        return new PjbIdempotencyStatusView(key, status(key));
    }

    public PjbIdempotencyTimelineResult timeline(String key) {
        Instant now = Instant.now();
        String currentStatus = status(key);
        Optional<PjbIdempotencyReplayPayload> payload = loadReplay(key);
        return new PjbIdempotencyTimelineResult(List.of(
                new PjbIdempotencyTimelineEntry(key, currentStatus == null ? "UNKNOWN" : currentStatus, now),
                new PjbIdempotencyTimelineEntry(key, payload.map(ignored -> "REPLAY_READY").orElse(currentStatus == null ? "UNKNOWN" : currentStatus), payload.map(PjbIdempotencyReplayPayload::completedAt).orElse(now))
        ));
    }

    public PjbIdempotencyWindowResult window(PjbIdempotencyWindowQuery query) {
        String currentStatus = status(query.key());
        return new PjbIdempotencyWindowResult(query.key(), retryAfterSeconds(), currentStatus != null);
    }

    public PjbIdempotencyAuditView auditView(String key) {
        return new PjbIdempotencyAuditView(key, status(key), Instant.now());
    }

    public PjbIdempotencyDecisionView decisionView(String key) {
        String currentStatus = status(key);
        return new PjbIdempotencyDecisionView(key, PROCESSING.equals(currentStatus), currentStatus);
    }

    public PjbIdempotencyReplayResult replay(PjbIdempotencyReplayQuery query) {
        String currentStatus = status(query.key());
        Optional<PjbIdempotencyReplayPayload> payload = loadReplay(query.key());
        return new PjbIdempotencyReplayResult(query.key(), payload.isPresent() || (currentStatus != null && !PROCESSING.equals(currentStatus)), currentStatus);
    }

    public PjbIdempotencyKeyHealthView keyHealthView(String key) {
        String currentStatus = status(key);
        return new PjbIdempotencyKeyHealthView(key, currentStatus != null, currentStatus);
    }

    public boolean acquire(String key) {
        boolean acquired = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(redisKey(key), PROCESSING, policy.ttl()));
        auditLedger.appendSafely("IDEMPOTENCY_ACQUIRE", "IDEMPOTENCY", key, null, "acquired=" + acquired);
        return acquired;
    }

    public void complete(String key, int status, String contentType, String body, String location) {
        PjbIdempotencyReplayPayload payload = new PjbIdempotencyReplayPayload(status, blankToNull(contentType), blankToNull(body), blankToNull(location), Instant.now());
        redis.opsForValue().set(redisKey(key), encode(payload), policy.ttl());
        auditLedger.appendSafely("IDEMPOTENCY_COMPLETE", "IDEMPOTENCY", key, null, "status=" + status + " location=" + nv(location));
    }

    public void markSuccess(String key) {
        complete(key, 200, null, null, null);
    }

    public void release(String key) {
        redis.delete(redisKey(key));
        auditLedger.appendSafely("IDEMPOTENCY_RELEASE", "IDEMPOTENCY", key, null, "released=true");
    }

    public String status(String key) {
        String stored = redis.opsForValue().get(redisKey(key));
        return statusFromStored(stored);
    }

    public Optional<PjbIdempotencyReplayPayload> loadReplay(String key) {
        return decode(redis.opsForValue().get(redisKey(key)));
    }

    public int retryAfterSeconds() {
        return policy.retryAfterSeconds();
    }

    private String statusFromStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        if (PROCESSING.equals(stored)) {
            return PROCESSING;
        }
        if (LEGACY_OK.equals(stored) || stored.startsWith(DONE_PREFIX)) {
            return LEGACY_OK;
        }
        return stored;
    }

    private String encode(PjbIdempotencyReplayPayload payload) {
        return DONE_PREFIX
                + payload.status() + '|'
                + encodePart(payload.contentType()) + '|'
                + encodePart(payload.location()) + '|'
                + encodePart(payload.body()) + '|'
                + payload.completedAt().toEpochMilli();
    }

    private Optional<PjbIdempotencyReplayPayload> decode(String stored) {
        if (stored == null || stored.isBlank() || PROCESSING.equals(stored) || LEGACY_OK.equals(stored) || !stored.startsWith(DONE_PREFIX)) {
            return Optional.empty();
        }
        String[] parts = stored.split("\\|", 6);
        if (parts.length != 6) {
            return Optional.empty();
        }
        try {
            return Optional.of(new PjbIdempotencyReplayPayload(
                    Integer.parseInt(parts[1]),
                    decodePart(parts[2]),
                    decodePart(parts[4]),
                    decodePart(parts[3]),
                    Instant.ofEpochMilli(Long.parseLong(parts[5]))
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String redisKey(String key) {
        return PREFIX + key;
    }

    private String encodePart(String value) {
        return Base64.getUrlEncoder().encodeToString(nv(value).getBytes(StandardCharsets.UTF_8));
    }

    private String decodePart(String value) {
        String decoded = new String(Base64.getUrlDecoder().decode(nv(value)), StandardCharsets.UTF_8);
        return decoded.isBlank() ? null : decoded;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String nv(String value) {
        return value == null ? "" : value;
    }
}
