package com.tcc.pjb.backend.core.frontend.app.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PjbFrontendOfficeGovernedProtocolSubmissionView(
        Long processoId,
        Long protocolPackageId,
        String integrityHash,
        String status,
        Long queueItemId,
        UUID submissionJobId,
        Long signerUserId,
        boolean queueRequired,
        String externalProtocolRef,
        LocalDateTime submittedAt,
        String lastError,
        String guardrailStatus,
        boolean readyForSubmission,
        List<String> blockers,
        List<String> warnings
) {
}
