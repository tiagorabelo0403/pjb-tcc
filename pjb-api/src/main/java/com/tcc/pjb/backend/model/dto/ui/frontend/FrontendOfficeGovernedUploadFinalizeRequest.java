package com.tcc.pjb.backend.model.dto.ui.frontend;

public record FrontendOfficeGovernedUploadFinalizeRequest(
        String expectedFingerprint,
        String idempotencyKey,
        String clientRequestId
) {
}
