package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.Map;

public record JudicialConnectorAdminOperationRequest(
        String operationType,
        JudicialSystem system,
        String tribunalCodigo,
        String environmentName,
        String requestedBy,
        String reason,
        Boolean productionReady,
        Boolean tribunalHomologated,
        Boolean tribunalBlocked,
        Boolean quarantineEnabled,
        Boolean maintenanceMode,
        String contractVersion,
        String certificateAlias,
        String submitPath,
        String dryRunPath,
        String snapshotPath,
        String eventsPath,
        String rolloutState,
        String notes,
        Instant validFrom,
        Instant validUntil,
        Map<String, Object> metadata
) {
}
