package com.tcc.pjb.backend.model.dto.processual.integration.external;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExternalIntegrationDiagnosticResponse(
        Long processoId,
        String numeroProcesso,
        String tribunalCodigo,
        String tribunalNome,
        String currentConnectorSystem,
        String selectedConnectorSystem,
        boolean connectorRegistered,
        boolean connectorOperational,
        boolean readyForSubmission,
        boolean readyForDryRun,
        boolean stepUpRequired,
        boolean certificateRequired,
        List<ConnectorOptionView> connectorLandscape,
        List<String> warnings,
        Map<String, Object> metadata,
        Instant diagnosedAt
) {
    public ExternalIntegrationDiagnosticResponse {
        connectorLandscape = connectorLandscape == null ? List.of() : List.copyOf(connectorLandscape);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        diagnosedAt = diagnosedAt == null ? Instant.now() : diagnosedAt;
    }

    public record ConnectorOptionView(
            String system,
            boolean enabled,
            boolean protocol,
            boolean dryRun,
            boolean snapshotSync,
            boolean eventSync,
            boolean operational,
            String baseUrl
    ) {
    }
}
