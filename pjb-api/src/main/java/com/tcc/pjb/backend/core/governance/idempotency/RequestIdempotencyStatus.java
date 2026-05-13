package com.tcc.pjb.backend.core.governance.idempotency;

public enum RequestIdempotencyStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    LOCKED
}
