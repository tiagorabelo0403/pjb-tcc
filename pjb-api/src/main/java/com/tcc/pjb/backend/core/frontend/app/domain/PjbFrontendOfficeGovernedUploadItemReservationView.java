package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;
import java.util.UUID;

public record PjbFrontendOfficeGovernedUploadItemReservationView(
        Long processoId,
        UUID batchId,
        UUID itemId,
        String uploadUrl,
        String itemStatus,
        String batchFingerprint,
        boolean allowed,
        List<String> blockers,
        List<String> warnings
) {
}
