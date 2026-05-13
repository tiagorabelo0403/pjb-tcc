package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiConversationEvidenceDescriptor(
        String evidenceId,
        String evidenceKind,
        String evidenceTier,
        String originType,
        String issuer,
        String sourceReference,
        String integrityHash,
        String signatureStatus,
        String extractionMode,
        boolean quarantined,
        List<String> quarantineReasons,
        List<String> downgradeReasons,
        boolean promotedForRag,
        boolean promotedForGrounding,
        boolean promotedForDraft,
        boolean promotedForSuggestion,
        boolean promotedForCapabilityRecovery
) {
    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfPresent(out, "evidenceId", evidenceId);
        putIfPresent(out, "evidenceKind", evidenceKind);
        putIfPresent(out, "evidenceTier", evidenceTier);
        putIfPresent(out, "originType", originType);
        putIfPresent(out, "issuer", issuer);
        putIfPresent(out, "sourceReference", sourceReference);
        putIfPresent(out, "integrityHash", integrityHash);
        putIfPresent(out, "signatureStatus", signatureStatus);
        putIfPresent(out, "extractionMode", extractionMode);
        out.put("quarantined", quarantined);
        out.put("quarantineReasons", quarantineReasons == null ? List.of() : List.copyOf(quarantineReasons));
        out.put("downgradeReasons", downgradeReasons == null ? List.of() : List.copyOf(downgradeReasons));
        out.put("promotedForRag", promotedForRag);
        out.put("promotedForGrounding", promotedForGrounding);
        out.put("promotedForDraft", promotedForDraft);
        out.put("promotedForSuggestion", promotedForSuggestion);
        out.put("promotedForCapabilityRecovery", promotedForCapabilityRecovery);
        return Collections.unmodifiableMap(out);
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        target.put(key, value);
    }
}
