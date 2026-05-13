package com.tcc.pjb.backend.model.dto.processual.recursal.protocolo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RecursalProtocolArtifactReadiness(
        String status,
        boolean protocolArtifactValid,
        boolean connectorSubmissionReady,
        boolean productionEvidenceReady,
        boolean readyForProduction,
        String evidenceProfile,
        List<String> reasons,
        Map<String, Object> details) {

    public RecursalProtocolArtifactReadiness {
        status = normalize(status);
        evidenceProfile = normalize(evidenceProfile);
        reasons = reasons == null || reasons.isEmpty() ? List.of() : List.copyOf(reasons);
        details = details == null || details.isEmpty() ? Map.of() : Map.copyOf(details);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("protocolArtifactValid", protocolArtifactValid);
        out.put("connectorSubmissionReady", connectorSubmissionReady);
        out.put("productionEvidenceReady", productionEvidenceReady);
        out.put("readyForProduction", readyForProduction);
        if (evidenceProfile != null) {
            out.put("evidenceProfile", evidenceProfile);
        }
        if (!reasons.isEmpty()) {
            out.put("reasons", reasons);
        }
        if (!details.isEmpty()) {
            out.put("details", details);
        }
        return Map.copyOf(out);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
