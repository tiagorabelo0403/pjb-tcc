package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloLatencyBudget(String operation, double targetSeconds) {}
