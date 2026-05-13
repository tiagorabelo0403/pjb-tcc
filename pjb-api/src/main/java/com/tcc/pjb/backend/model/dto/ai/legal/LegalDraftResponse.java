package com.tcc.pjb.backend.model.dto.ai.legal;

import java.util.List;
import java.util.Map;

public record LegalDraftResponse(
        String minuta,
        String status,
        List<String> nextSteps,
        Map<String, Object> safeguards
) {
}
