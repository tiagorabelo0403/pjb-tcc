package com.tcc.pjb.backend.platform.runtime.domain;

public record PjbRuntimeSizingView(
        int availableProcessors,
        long maxMemoryMiB,
        String componentRole
) {
}
