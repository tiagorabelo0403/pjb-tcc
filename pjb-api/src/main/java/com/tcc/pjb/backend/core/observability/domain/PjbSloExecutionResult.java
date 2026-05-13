package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloExecutionResult(
        String operation,
        double measuredSeconds,
        double sloSeconds,
        boolean violated
) {}
