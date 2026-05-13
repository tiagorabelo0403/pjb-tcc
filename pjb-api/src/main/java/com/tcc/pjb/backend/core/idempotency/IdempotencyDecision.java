package com.tcc.pjb.backend.core.idempotency;

public enum IdempotencyDecision {
    NEW,
    REPLAY,
    IN_PROGRESS
}
