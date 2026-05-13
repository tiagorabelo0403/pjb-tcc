package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloExecutionQuery(
        String operation,
        double measuredSeconds
) {}
