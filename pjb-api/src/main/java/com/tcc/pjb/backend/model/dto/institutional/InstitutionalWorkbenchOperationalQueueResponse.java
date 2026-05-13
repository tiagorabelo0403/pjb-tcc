package com.tcc.pjb.backend.model.dto.institutional;

import java.time.Instant;
import java.util.List;

public record InstitutionalWorkbenchOperationalQueueResponse(
        Instant generatedAt,
        String actorClass,
        int requestedLimit,
        int totalItems,
        int actionableItems,
        int blockedItems,
        List<InstitutionalWorkbenchQueueItemResponse> items,
        List<String> warnings
) {
}
