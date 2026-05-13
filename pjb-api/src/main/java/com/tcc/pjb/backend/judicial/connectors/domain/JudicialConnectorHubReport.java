package com.tcc.pjb.backend.judicial.connectors.domain;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCryptoCommandCenterReport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorHubReport(
        Instant generatedAt,
        String tribunalCodigo,
        JudicialConnectorStructureReport structure,
        JudicialConnectorCommandCenterReport operational,
        JudicialConnectorCryptoCommandCenterReport cryptography,
        List<String> alerts,
        Map<String, Object> metadata
) {
    public JudicialConnectorHubReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt.toString());
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("structure", structure != null ? structure.toMap() : Map.of());
        out.put("operational", operational != null ? operational.toMap() : Map.of());
        out.put("cryptography", cryptography != null ? cryptography.toMap() : Map.of());
        out.put("alerts", alerts);
        out.put("metadata", metadata);
        return Map.copyOf(out);
    }
}
