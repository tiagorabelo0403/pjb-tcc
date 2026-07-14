package com.tcc.pjb.backend.core.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionIdempotencyService {

    private final JdbcTemplate jdbc;

    public ActionIdempotencyService(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Transactional
    public IdempotencyBeginResult begin(String scope, String idempotencyKey, String requestHash, Duration lockTtl) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(idempotencyKey);
        Objects.requireNonNull(requestHash);

        Instant now = Instant.now();
        Instant lockUntil = now.plus(lockTtl != null ? lockTtl : Duration.ofSeconds(30));

        try {
            jdbc.update(
                    "INSERT INTO tb_idempotency(scope, idempotency_key, status, request_hash, lock_until, created_at, updated_at) VALUES (?,?,?,?,?,?,?)",
                    scope, idempotencyKey, IdempotencyStatus.IN_PROGRESS.name(), requestHash,
                    java.sql.Timestamp.from(lockUntil), java.sql.Timestamp.from(now), java.sql.Timestamp.from(now)
            );
            return new IdempotencyBeginResult(IdempotencyDecision.NEW, IdempotencyStatus.IN_PROGRESS, scope, idempotencyKey, requestHash, null, null, null);
        } catch (DataIntegrityViolationException dup) {
            return resolveExisting(scope, idempotencyKey, requestHash, lockUntil, now);
        }
    }

    private IdempotencyBeginResult resolveExisting(String scope, String idempotencyKey, String requestHash, Instant newLockUntil, Instant now) {
        return jdbc.query(
                "SELECT status, request_hash, resource_type, resource_id, response_json, lock_until FROM tb_idempotency WHERE scope=? AND idempotency_key=?",
                ps -> {
                    ps.setString(1, scope);
                    ps.setString(2, idempotencyKey);
                },
                rs -> {
                    if (!rs.next()) {
                        return new IdempotencyBeginResult(IdempotencyDecision.NEW, IdempotencyStatus.IN_PROGRESS, scope, idempotencyKey, requestHash, null, null, null);
                    }

                    IdempotencyStatus st = IdempotencyStatus.valueOf(rs.getString(1));
                    String storedReqHash = rs.getString(2);
                    String resourceType = rs.getString(3);
                    String resourceId = rs.getString(4);
                    String responseJson = rs.getString(5);
                    java.sql.Timestamp ts = rs.getTimestamp(6);
                    Instant lockUntil = ts != null ? ts.toInstant() : null;

                    if (st == IdempotencyStatus.COMPLETED) {
                        return new IdempotencyBeginResult(IdempotencyDecision.REPLAY, st, scope, idempotencyKey, storedReqHash, resourceType, resourceId, responseJson);
                    }

                    if (st == IdempotencyStatus.IN_PROGRESS) {
                        if (lockUntil != null && lockUntil.isBefore(now)) {
                            int updated = jdbc.update(
                                    "UPDATE tb_idempotency SET request_hash=?, lock_until=?, updated_at=? WHERE scope=? AND idempotency_key=? AND status='IN_PROGRESS' AND (lock_until IS NULL OR lock_until < ?)",
                                    requestHash, java.sql.Timestamp.from(newLockUntil), java.sql.Timestamp.from(now),
                                    scope, idempotencyKey, java.sql.Timestamp.from(now)
                            );
                            if (updated > 0) {
                                return new IdempotencyBeginResult(IdempotencyDecision.NEW, IdempotencyStatus.IN_PROGRESS, scope, idempotencyKey, requestHash, null, null, null);
                            }
                        }
                        return new IdempotencyBeginResult(IdempotencyDecision.IN_PROGRESS, st, scope, idempotencyKey, storedReqHash, resourceType, resourceId, responseJson);
                    }

                    
                    if (lockUntil != null && lockUntil.isBefore(now)) {
                        int updated = jdbc.update(
                                "UPDATE tb_idempotency SET status='IN_PROGRESS', request_hash=?, lock_until=?, updated_at=? WHERE scope=? AND idempotency_key=? AND status='FAILED'",
                                requestHash, java.sql.Timestamp.from(newLockUntil), java.sql.Timestamp.from(now), scope, idempotencyKey
                        );
                        if (updated > 0) {
                            return new IdempotencyBeginResult(IdempotencyDecision.NEW, IdempotencyStatus.IN_PROGRESS, scope, idempotencyKey, requestHash, null, null, null);
                        }
                    }
                    return new IdempotencyBeginResult(IdempotencyDecision.IN_PROGRESS, st, scope, idempotencyKey, storedReqHash, resourceType, resourceId, responseJson);
                }
        );
    }

    @Transactional
    public void complete(String scope, String idempotencyKey, String responseHash, String resourceType, String resourceId, String responseJson) {
        Instant now = Instant.now();
        jdbc.update(
                "UPDATE tb_idempotency SET status='COMPLETED', response_hash=?, resource_type=?, resource_id=?, response_json=?, lock_until=NULL, updated_at=? WHERE scope=? AND idempotency_key=?",
                responseHash, resourceType, resourceId, responseJson, java.sql.Timestamp.from(now), scope, idempotencyKey
        );
    }

    @Transactional
    public void fail(String scope, String idempotencyKey, String responseHash, String responseJson) {
        Instant now = Instant.now();
        jdbc.update(
                "UPDATE tb_idempotency SET status='FAILED', response_hash=?, response_json=?, lock_until=NULL, updated_at=? WHERE scope=? AND idempotency_key=?",
                responseHash, responseJson, java.sql.Timestamp.from(now), scope, idempotencyKey
        );
    }
}
