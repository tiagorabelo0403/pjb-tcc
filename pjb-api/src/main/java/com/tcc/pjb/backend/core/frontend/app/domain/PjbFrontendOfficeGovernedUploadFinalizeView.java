package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;
import java.util.UUID;

public record PjbFrontendOfficeGovernedUploadFinalizeView(
        Long processoId,
        UUID batchId,
        UUID jobId,
        String status,
        boolean replay,
        boolean inProgress,
        String batchFingerprint,
        boolean allowed,
        List<String> blockers,
        List<String> warnings
) {
}
