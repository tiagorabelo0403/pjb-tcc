package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbCoreSeedExtractionSnapshot(
        boolean sourcePackagePresent,
        boolean moduleMirrorPresent,
        boolean parityReady,
        int sourceFileCount,
        int mirroredFileCount,
        int alignedFileCount,
        List<String> blockers,
        Instant checkedAt
) {

    public PjbCoreSeedExtractionSnapshot {
        blockers = List.copyOf(Objects.requireNonNull(blockers));
        checkedAt = Objects.requireNonNull(checkedAt);
    }
}
