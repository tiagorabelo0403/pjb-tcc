package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloConsistencyView(
        String operation,
        boolean consistent,
        String summary,
        String source
) {}
