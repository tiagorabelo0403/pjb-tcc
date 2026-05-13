package com.tcc.pjb.backend.core.observability.domain;

import java.time.Instant;
import java.util.List;

public record PjbSloRegistrySnapshot(List<PjbSloOperationView> operations, Instant capturedAt) {}
