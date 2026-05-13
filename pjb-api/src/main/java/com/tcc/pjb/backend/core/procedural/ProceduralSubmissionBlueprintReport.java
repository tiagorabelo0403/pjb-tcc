package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralSubmissionBlueprintReport(
        Instant generatedAt,
        String requestId,
        String blueprintStatus,
        boolean readyForAssistedSubmission,
        boolean readyForRealConnectorSubmission,
        boolean dryRunAccepted,
        JudicialSystem judicialSystem,
        String tribunalCodigo,
        String tribunalNome,
        String classeTpuCodigo,
        String classeTpuNome,
        String unidadeJudiciariaCodigo,
        String unidadeJudiciariaNome,
        String rito,
        String actionNature,
        String territorialMode,
        String preventionMode,
        String linkageMode,
        String localCorrelationMode,
        List<String> relatedLocalProcessNumbers,
        boolean connectorOperational,
        boolean requiresGovBrStepUp,
        boolean requiresCertificate,
        String dryRunStatus,
        String dryRunReference,
        List<String> blockingIssues,
        List<String> reviewChecklist,
        List<String> warnings,
        Map<String, Object> protocolRequestPreview,
        Map<String, Object> metadata
) {

    public boolean preProtocoloApto() {
        return readyForAssistedSubmission || readyForRealConnectorSubmission;
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("requestId", requestId);
        out.put("blueprintStatus", blueprintStatus);
        out.put("readyForAssistedSubmission", readyForAssistedSubmission);
        out.put("readyForRealConnectorSubmission", readyForRealConnectorSubmission);
        out.put("dryRunAccepted", dryRunAccepted);
        out.put("judicialSystem", judicialSystem != null ? judicialSystem.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("tribunalNome", tribunalNome);
        out.put("classeTpuCodigo", classeTpuCodigo);
        out.put("classeTpuNome", classeTpuNome);
        out.put("unidadeJudiciariaCodigo", unidadeJudiciariaCodigo);
        out.put("unidadeJudiciariaNome", unidadeJudiciariaNome);
        out.put("rito", rito);
        out.put("actionNature", actionNature);
        out.put("territorialMode", territorialMode);
        out.put("preventionMode", preventionMode);
        out.put("linkageMode", linkageMode);
        out.put("localCorrelationMode", localCorrelationMode);
        out.put("relatedLocalProcessNumbers", relatedLocalProcessNumbers == null ? List.of() : relatedLocalProcessNumbers);
        out.put("connectorOperational", connectorOperational);
        out.put("requiresGovBrStepUp", requiresGovBrStepUp);
        out.put("requiresCertificate", requiresCertificate);
        out.put("dryRunStatus", dryRunStatus);
        out.put("dryRunReference", dryRunReference);
        out.put("blockingIssues", blockingIssues == null ? List.of() : blockingIssues);
        out.put("reviewChecklist", reviewChecklist == null ? List.of() : reviewChecklist);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("protocolRequestPreview", protocolRequestPreview == null ? Map.of() : protocolRequestPreview);
        out.put("metadata", metadata == null ? Map.of() : metadata);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
