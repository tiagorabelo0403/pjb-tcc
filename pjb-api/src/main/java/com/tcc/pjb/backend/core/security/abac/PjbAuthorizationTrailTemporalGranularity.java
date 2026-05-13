package com.tcc.pjb.backend.core.security.abac;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public enum PjbAuthorizationTrailTemporalGranularity {
    HOUR,
    DAY;

    public Instant bucketStart(Instant value) {
        ZonedDateTime dateTime = (value == null ? Instant.now() : value).atZone(ZoneOffset.UTC);
        return switch (this) {
            case HOUR -> dateTime.withMinute(0).withSecond(0).withNano(0).toInstant();
            case DAY -> dateTime.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        };
    }

    public Instant bucketEndExclusive(Instant value) {
        ZonedDateTime start = bucketStart(value).atZone(ZoneOffset.UTC);
        return switch (this) {
            case HOUR -> start.plusHours(1).toInstant();
            case DAY -> start.plusDays(1).toInstant();
        };
    }
}
