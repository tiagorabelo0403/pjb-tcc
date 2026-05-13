package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloOperationView(String operation, boolean registered, long count) {}
