package com.tcc.pjb.backend.model.dto.ai.legal.spine;

import java.util.List;
import java.util.Map;

public record LegalAiApprovalDescriptor(
        boolean approvalRequired,
        boolean stepUpRequired,
        List<String> approvalReasons,
        Map<String, Object> approvalPolicy
) {
    public Map<String, Object> asMap() {
        return Map.of(
                "approvalRequired", approvalRequired,
                "stepUpRequired", stepUpRequired,
                "approvalReasons", approvalReasons == null ? List.of() : List.copyOf(approvalReasons),
                "approvalPolicy", approvalPolicy == null ? Map.of() : Map.copyOf(approvalPolicy)
        );
    }
}
