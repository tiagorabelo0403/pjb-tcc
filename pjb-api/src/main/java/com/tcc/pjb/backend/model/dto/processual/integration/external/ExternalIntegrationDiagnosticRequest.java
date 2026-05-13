package com.tcc.pjb.backend.model.dto.processual.integration.external;

import jakarta.validation.constraints.NotNull;

public record ExternalIntegrationDiagnosticRequest(
        @NotNull Long processoId,
        boolean includeConnectorLandscape,
        boolean includeSubmissionReadiness
) {
}
