package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record JudicialConnectorPolicyOverlay(
        UUID policyId,
        JudicialSystem system,
        String environmentName,
        String tribunalCodigo,
        boolean policyPresent,
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
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public JudicialConnectorPolicyOverlay(UUID policyId,
                                          JudicialSystem system,
                                          String environmentName,
                                          String tribunalCodigo,
                                          boolean policyPresent,
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
                                          List<String> blockers,
                                          List<String> warnings,
                                          Map<String, Object> metadata) {
        this(policyId, system, environmentName, tribunalCodigo, policyPresent, productionReady, tribunalHomologated, tribunalBlocked, quarantineEnabled, maintenanceMode, contractVersion, certificateAlias, submitPath, dryRunPath, snapshotPath, eventsPath, rolloutState, approvedBy, reason, notes, validFrom, null, blockers, warnings, metadata);
    }

    public static JudicialConnectorPolicyOverlay none(JudicialSystem system, String environmentName, String tribunalCodigo) {
        return new JudicialConnectorPolicyOverlay(null, system, environmentName, tribunalCodigo, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of(), List.of(), Map.of());
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("policyId", policyId != null ? policyId.toString() : null);
        out.put("system", system != null ? system.name() : null);
        out.put("environmentName", environmentName);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("policyPresent", policyPresent);
        out.put("productionReady", productionReady);
        out.put("tribunalHomologated", tribunalHomologated);
        out.put("tribunalBlocked", tribunalBlocked);
        out.put("quarantineEnabled", quarantineEnabled);
        out.put("maintenanceMode", maintenanceMode);
        out.put("contractVersion", contractVersion);
        out.put("certificateAlias", certificateAlias);
        out.put("submitPath", submitPath);
        out.put("dryRunPath", dryRunPath);
        out.put("snapshotPath", snapshotPath);
        out.put("eventsPath", eventsPath);
        out.put("rolloutState", rolloutState);
        out.put("approvedBy", approvedBy);
        out.put("reason", reason);
        out.put("notes", notes);
        out.put("validFrom", validFrom != null ? validFrom.toString() : null);
        out.put("validUntil", validUntil != null ? validUntil.toString() : null);
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
