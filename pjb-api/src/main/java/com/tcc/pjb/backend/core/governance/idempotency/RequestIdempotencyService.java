package com.tcc.pjb.backend.core.governance.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.governance.idempotency.persistence.entity.RequestIdempotencyEntity;
import com.tcc.pjb.backend.core.governance.idempotency.persistence.repo.RequestIdempotencyRepository;

@Service
public class RequestIdempotencyService {

    private final RequestIdempotencyRepository repository;

    public RequestIdempotencyService(RequestIdempotencyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RequestIdempotencyBeginResult begin(String action, String requestHash, Duration inProgressTtl) {
        Objects.requireNonNull(requestHash, "requestHash");
        String rh = requestHash.trim();
        if (rh.isEmpty()) throw new IllegalArgumentException("requestHash blank");

        Instant now = Instant.now();
        Instant lockUntil = inProgressTtl == null ? now.plus(Duration.ofMinutes(2)) : now.plus(inProgressTtl);

        RequestIdempotencyEntity e = repository.findForUpdate(rh).orElse(null);

        if (e == null) {
            RequestIdempotencyEntity created = new RequestIdempotencyEntity();
            created.setRequestHash(rh);
            created.setStatus(RequestIdempotencyStatus.IN_PROGRESS);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            created.setLockUntil(lockUntil);
            repository.save(created);
            return new RequestIdempotencyBeginResult(created.getStatus(), true, null, null, null, null);
        }

        
        if (e.getStatus() == RequestIdempotencyStatus.IN_PROGRESS && e.getLockUntil() != null && e.getLockUntil().isBefore(now)) {
            e.setLockUntil(lockUntil);
            e.setUpdatedAt(now);
            repository.save(e);
            return new RequestIdempotencyBeginResult(e.getStatus(), false, e.getResourceType(), e.getResourceId(), e.getResponseHash(), e.getResponseJson());
        }

        if (e.getStatus() == RequestIdempotencyStatus.IN_PROGRESS) {
            throw new IdempotencyInProgressException(action == null ? "action" : action, rh);
        }

        return new RequestIdempotencyBeginResult(e.getStatus(), false, e.getResourceType(), e.getResourceId(), e.getResponseHash(), e.getResponseJson());
    }

    @Transactional
    public void complete(String requestHash, String resourceType, String resourceId, String responseHash, String responseJson) {
        finish(requestHash, RequestIdempotencyStatus.COMPLETED, resourceType, resourceId, responseHash, responseJson);
    }

    @Transactional
    public void lock(String requestHash, String resourceType, String resourceId, String responseHash, String responseJson) {
        finish(requestHash, RequestIdempotencyStatus.LOCKED, resourceType, resourceId, responseHash, responseJson);
    }

    private void finish(String requestHash,
                        RequestIdempotencyStatus status,
                        String resourceType,
                        String resourceId,
                        String responseHash,
                        String responseJson) {
        Objects.requireNonNull(requestHash, "requestHash");
        String rh = requestHash.trim();

        Instant now = Instant.now();
        RequestIdempotencyEntity e = repository.findForUpdate(rh).orElseThrow(() -> new IllegalStateException("idempotency record missing"));
        e.setStatus(status);
        e.setUpdatedAt(now);
        e.setLockUntil(null);
        e.setResourceType(resourceType);
        e.setResourceId(resourceId);
        e.setResponseHash(responseHash);
        e.setResponseJson(responseJson);
        repository.save(e);
    }

    @Transactional
    public void fail(String requestHash) {
        if (requestHash == null || requestHash.isBlank()) return;
        String rh = requestHash.trim();
        Instant now = Instant.now();
        RequestIdempotencyEntity e = repository.findForUpdate(rh).orElse(null);
        if (e == null) return;
        e.setStatus(RequestIdempotencyStatus.FAILED);
        e.setUpdatedAt(now);
        e.setLockUntil(null);
        repository.save(e);
    }
}
