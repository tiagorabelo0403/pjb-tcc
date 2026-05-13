package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;
import java.util.UUID;

public record PjbFrontendOfficeGovernedDocumentBatchPreviewView(
        Long processoId,
        UUID batchId,
        String batchStatus,
        Integer expectedCount,
        int itemCount,
        long uploadedCount,
        long reservedCount,
        long linkedCount,
        long failedCount,
        long totalBytes,
        String batchFingerprint,
        String workspaceMode,
        Long activeEquipeId,
        boolean accessAllowed,
        boolean queueRequired,
        Long effectiveSignerUserId,
        String effectiveSignerNome,
        boolean allowed,
        List<String> blockers,
        List<String> warnings
) {
}
