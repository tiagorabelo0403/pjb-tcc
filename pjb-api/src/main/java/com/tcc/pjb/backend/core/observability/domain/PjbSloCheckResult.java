package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloCheckResult(String operation, boolean registered, long count) {}
