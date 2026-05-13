package com.tcc.pjb.backend.core.governance.idempotency;


public record RequestIdempotencyBeginResult(
        RequestIdempotencyStatus status,
        boolean created,
        String resourceType,
        String resourceId,
        String responseHash,
        String responseJson
) {
    public boolean isCompleted() {
        return status == RequestIdempotencyStatus.COMPLETED;
    }

    public boolean isInProgress() {
        return status == RequestIdempotencyStatus.IN_PROGRESS;
    }
}
