package com.tcc.pjb.backend.core.observability.unavailability;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record PjbSystemUnavailabilityEvent(
        String tribunalCode,
        Instant startedAt,
        Instant endedAt,
        Set<PjbUnavailableService> affectedServices,
        boolean planned
) {
    public PjbSystemUnavailabilityEvent {
        tribunalCode = Objects.toString(tribunalCode, "").trim().toUpperCase();
        startedAt = startedAt == null ? Instant.EPOCH : startedAt;
        endedAt = endedAt == null ? startedAt : endedAt;
        affectedServices = affectedServices == null ? Set.of() : Set.copyOf(affectedServices);
    }

    public Duration duration() {
        return endedAt.isAfter(startedAt) ? Duration.between(startedAt, endedAt) : Duration.ZERO;
    }

    public boolean affectsExternalUser() {
        return affectedServices.contains(PjbUnavailableService.DIGITAL_CASE_FILE)
                || affectedServices.contains(PjbUnavailableService.ELECTRONIC_FILING)
                || affectedServices.contains(PjbUnavailableService.ELECTRONIC_NOTICE)
                || affectedServices.contains(PjbUnavailableService.PUBLIC_CONSULTATION);
    }
}
