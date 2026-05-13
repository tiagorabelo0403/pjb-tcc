package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;
import java.util.UUID;

public record PjbFrontendOfficeGovernedUploadIngressView(
        Long processoId,
        UUID batchId,
        UUID itemId,
        String itemStatus,
        String sha256,
        String sha384,
        String storageUri,
        String batchFingerprint,
        boolean allowed,
        List<String> blockers,
        List<String> warnings
) {
}
