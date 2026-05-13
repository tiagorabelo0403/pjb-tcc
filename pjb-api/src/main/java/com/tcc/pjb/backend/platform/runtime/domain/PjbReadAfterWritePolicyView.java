package com.tcc.pjb.backend.platform.runtime.domain;

public record PjbReadAfterWritePolicyView(
        long windowMillis,
        boolean requestScoped,
        String forceRoute
) {
}
