package com.tcc.pjb.backend.platform.security.idempotency.domain;

import java.util.List;

public record PjbIdempotencyTimelineResult(List<PjbIdempotencyTimelineEntry> entries) {}
