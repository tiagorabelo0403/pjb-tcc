package com.tcc.pjb.backend.platform.runtime.domain;

public record PjbReadAfterWriteRequestView(
        boolean writeMarked,
        boolean forcePrimary,
        Long lastWriteAtEpochMillis,
        long windowMillis
) {
}
