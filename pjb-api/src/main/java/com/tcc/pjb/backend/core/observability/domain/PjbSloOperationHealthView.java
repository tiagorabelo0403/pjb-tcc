package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloOperationHealthView(String operation, boolean registered, double targetSeconds) {}
