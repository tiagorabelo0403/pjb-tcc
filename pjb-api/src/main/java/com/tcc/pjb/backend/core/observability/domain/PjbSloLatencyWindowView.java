package com.tcc.pjb.backend.core.observability.domain;

import java.time.Instant;

public record PjbSloLatencyWindowView(String operation, double targetSeconds, Instant generatedAt) {}
