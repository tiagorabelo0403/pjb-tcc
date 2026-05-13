package com.tcc.pjb.backend.model.dto.secretariat.governance;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecretariatFormalCatalogSnapshotDto(
        Instant generatedAt,
        String inboxKey,
        String ramoAxis,
        String ritoAxis,
        String instanceAxis,
        Map<String, Object> metrics,
        List<DocumentTemplate> documents,
        List<String> warnings,
        Map<String, Object> routes
) {
    public record DocumentTemplate(
            String documentCode,
            String title,
            String actAxis,
            String targetBranch,
            String targetPhase,
            String urgencyAxis,
            boolean sensitive,
            List<String> tags,
            Map<String, Object> metadata
    ) {
    }
}
