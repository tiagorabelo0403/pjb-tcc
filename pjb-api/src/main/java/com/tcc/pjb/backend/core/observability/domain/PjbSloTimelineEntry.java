package com.tcc.pjb.backend.core.observability.domain;

import java.time.Instant;

public record PjbSloTimelineEntry(String operation, String event, Instant at) {}
