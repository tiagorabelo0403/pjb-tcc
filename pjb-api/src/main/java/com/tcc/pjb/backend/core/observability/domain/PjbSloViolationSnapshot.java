package com.tcc.pjb.backend.core.observability.domain;

import java.time.Instant;

public record PjbSloViolationSnapshot(String operation, double targetSeconds, double observedSeconds, Instant observedAt) {}
