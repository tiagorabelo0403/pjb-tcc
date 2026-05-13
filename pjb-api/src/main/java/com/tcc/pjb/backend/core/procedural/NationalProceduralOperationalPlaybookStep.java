package com.tcc.pjb.backend.core.procedural;

import java.util.List;

public record NationalProceduralOperationalPlaybookStep(
        int orderIndex,
        String code,
        String lane,
        String title,
        boolean blocking,
        List<String> outputs
) {
    public NationalProceduralOperationalPlaybookStep {
        orderIndex = Math.max(orderIndex, 1);
        code = normalize(code, "UNSPECIFIED");
        lane = normalize(lane, "OPERACIONAL");
        title = normalize(title, "Etapa operacional");
        outputs = NationalProceduralRecordSupport.copyList(outputs);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
