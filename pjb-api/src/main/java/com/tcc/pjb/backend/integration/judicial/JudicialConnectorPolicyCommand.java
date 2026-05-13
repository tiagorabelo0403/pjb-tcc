package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record JudicialConnectorPolicyCommand(
        UUID policyId,
        JudicialSystem system,
        String environmentName,
        String tribunalCodigo,
        Boolean active,
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
        String approvedBy,
        String reason,
        String notes,
        Instant validFrom,
        Instant validUntil,
        Map<String, Object> metadata
) {
}
