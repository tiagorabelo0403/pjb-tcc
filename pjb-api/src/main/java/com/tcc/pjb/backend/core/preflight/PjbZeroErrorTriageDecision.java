package com.tcc.pjb.backend.core.preflight;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;

public record PjbZeroErrorTriageDecision(
        RitoProcessual canonicalRito,
        String status,
        int urgencyScore,
        boolean canProtocol,
        boolean requiresHumanReview,
        boolean costsBlocked,
        List<PjbZeroErrorTriageIssue> issues,
        List<String> suggestedFirstMovements
) {
}
