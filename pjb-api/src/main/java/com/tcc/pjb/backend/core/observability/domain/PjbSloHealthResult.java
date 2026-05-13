package com.tcc.pjb.backend.core.observability.domain;

import java.time.Instant;

public record PjbSloHealthResult(String operation, boolean registered, long count, Instant checkedAt) {}
