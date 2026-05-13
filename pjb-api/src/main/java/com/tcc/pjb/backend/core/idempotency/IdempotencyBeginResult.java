package com.tcc.pjb.backend.core.idempotency;

import java.util.Optional;

public record IdempotencyBeginResult(
        IdempotencyDecision decision,
        IdempotencyStatus status,
        String scope,
        String idempotencyKey,
        String requestHash,
        String resourceType,
        String resourceId,
        String responseJson
) {
    public Optional<String> resourceTypeOptional() {
        return Optional.ofNullable(resourceType);
    }

    public Optional<String> resourceIdOptional() {
        return Optional.ofNullable(resourceId);
    }

    public Optional<String> responseJsonOptional() {
        return Optional.ofNullable(responseJson);
    }
}
