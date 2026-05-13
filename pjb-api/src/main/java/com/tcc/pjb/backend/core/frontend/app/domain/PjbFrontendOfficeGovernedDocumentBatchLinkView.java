package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;
import java.util.UUID;

public record PjbFrontendOfficeGovernedDocumentBatchLinkView(
        Long processoId,
        UUID batchId,
        String status,
        Long operationId,
        Long queueItemId,
        Long signerUserId,
        String delegationMode,
        Integer linkedCount,
        List<UUID> linkedDocumentIds,
        String batchFingerprint,
        boolean queueRequired,
        Long effectiveSignerUserId,
        String effectiveSignerNome,
        List<String> workspaceWarnings,
        List<String> operationWarnings
) {
}
