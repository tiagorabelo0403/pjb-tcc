package com.tcc.pjb.backend.core.observability.domain;

import java.time.Instant;

public record PjbSloRegistryHealthView(boolean available, int operationCount, Instant generatedAt) {}
