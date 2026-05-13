package com.tcc.pjb.backend.platform.runtime.execution;

import java.time.Duration;
import java.util.Objects;

public record PjbExecutionDescriptor(String operationName,
                                     PjbExecutionLane lane,
                                     Duration timeout,
                                     boolean critical) {

    public PjbExecutionDescriptor {
        operationName = normalizeOperationName(operationName);
        lane = lane == null ? PjbExecutionLane.IO : lane;
        timeout = normalizeTimeout(timeout);
    }

    public static PjbExecutionDescriptor io(String operationName, Duration timeout) {
        return new PjbExecutionDescriptor(operationName, PjbExecutionLane.IO, timeout, false);
    }

    public static PjbExecutionDescriptor burst(String operationName, Duration timeout) {
        return new PjbExecutionDescriptor(operationName, PjbExecutionLane.BURST, timeout, false);
    }

    public static PjbExecutionDescriptor externalIo(String operationName, Duration timeout) {
        return new PjbExecutionDescriptor(operationName, PjbExecutionLane.EXTERNAL_IO, timeout, true);
    }

    public static PjbExecutionDescriptor live(String operationName, Duration timeout) {
        return new PjbExecutionDescriptor(operationName, PjbExecutionLane.LIVE, timeout, true);
    }

    public static PjbExecutionDescriptor job(String operationName, Duration timeout) {
        return new PjbExecutionDescriptor(operationName, PjbExecutionLane.JOB, timeout, false);
    }

    private static String normalizeOperationName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("operationName is required");
        }
        return value.trim();
    }

    private static Duration normalizeTimeout(Duration value) {
        if (value == null || value.isNegative() || value.isZero()) {
            return Duration.ofSeconds(5);
        }
        return value;
    }
}
