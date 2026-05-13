package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralConnectorExecutionReport(
        Instant generatedAt,
        String executionMode,
        String submissionLane,
        String tribunalTargetKey,
        JudicialSystem judicialSystem,
        String tribunalCodigo,
        String unidadeJudiciariaCodigo,
        String protocolClassCode,
        String idempotencyKey,
        String signerMode,
        String retryPolicy,
        boolean allowedToAutoSubmit,
        boolean requiresHumanReview,
        boolean requiresGovBrStepUp,
        boolean requiresCertificate,
        List<String> phases,
        List<String> executionChecklist,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {

    public boolean connectorReady() {
        return allowedToAutoSubmit;
    }

    public List<String> reviewChecklist() {
        return executionChecklist == null ? List.of() : executionChecklist;
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("executionMode", executionMode);
        out.put("submissionLane", submissionLane);
        out.put("tribunalTargetKey", tribunalTargetKey);
        out.put("judicialSystem", judicialSystem != null ? judicialSystem.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("unidadeJudiciariaCodigo", unidadeJudiciariaCodigo);
        out.put("protocolClassCode", protocolClassCode);
        out.put("idempotencyKey", idempotencyKey);
        out.put("signerMode", signerMode);
        out.put("retryPolicy", retryPolicy);
        out.put("allowedToAutoSubmit", allowedToAutoSubmit);
        out.put("requiresHumanReview", requiresHumanReview);
        out.put("requiresGovBrStepUp", requiresGovBrStepUp);
        out.put("requiresCertificate", requiresCertificate);
        out.put("phases", phases == null ? List.of() : phases);
        out.put("executionChecklist", executionChecklist == null ? List.of() : executionChecklist);
        out.put("blockers", blockers == null ? List.of() : blockers);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
