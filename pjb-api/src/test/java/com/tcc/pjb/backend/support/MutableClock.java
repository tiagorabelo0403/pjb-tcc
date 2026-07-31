package com.tcc.pjb.backend.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

public final class MutableClock extends Clock {

    private Instant current;

    public MutableClock(Instant current) {
        this.current = current;
    }

    @Override
    public ZoneOffset getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return current;
    }

    public void advance(java.time.Duration duration) {
        current = current.plus(duration);
    }

    public void set(Instant instant) {
        current = Objects.requireNonNull(instant);
    }
}
