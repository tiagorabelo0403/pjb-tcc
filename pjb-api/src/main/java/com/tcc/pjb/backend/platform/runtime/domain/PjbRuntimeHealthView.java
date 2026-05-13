package com.tcc.pjb.backend.platform.runtime.domain;

public record PjbRuntimeHealthView(
        String status,
        boolean ready,
        boolean draining,
        String summary
) {
}
