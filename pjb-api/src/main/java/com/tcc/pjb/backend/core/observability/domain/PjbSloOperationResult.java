package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloOperationResult(String operation, boolean exists, long count) {}
