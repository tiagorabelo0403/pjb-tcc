package com.tcc.pjb.backend.model.dto.ai.legal.surface;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceDescriptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LegalAiStructuredSurfaceEvidenceBundle(
        String surfaceCode,
        String promotionStatus,
        boolean anchored,
        List<String> promotedEvidenceIds,
        List<LegalAiConversationEvidenceDescriptor> promotedEvidenceDescriptors,
        List<String> unmetRequirements,
        List<String> reasons
) {

    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfPresent(out, "surfaceCode", surfaceCode);
        putIfPresent(out, "promotionStatus", promotionStatus);
        out.put("anchored", anchored);
        out.put("promotedEvidenceIds", promotedEvidenceIds == null ? List.of() : List.copyOf(promotedEvidenceIds));
        out.put("promotedEvidenceDescriptors", promotedEvidenceDescriptors == null
                ? List.of()
                : promotedEvidenceDescriptors.stream().map(LegalAiConversationEvidenceDescriptor::asMap).toList());
        out.put("unmetRequirements", unmetRequirements == null ? List.of() : List.copyOf(unmetRequirements));
        out.put("reasons", reasons == null ? List.of() : List.copyOf(reasons));
        return Collections.unmodifiableMap(out);
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        target.put(key, value);
    }
}
