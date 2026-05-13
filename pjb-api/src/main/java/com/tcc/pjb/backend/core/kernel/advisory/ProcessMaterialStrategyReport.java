package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.List;
import java.util.Map;

public record ProcessMaterialStrategyReport(
        String litigationPosture,
        String protocolReadiness,
        String negotiationStance,
        String evidenceReadiness,
        List<String> pleadingBlueprint,
        List<String> evidenceAgenda,
        List<String> protocolBlockers,
        List<String> negotiationGuardrails,
        List<String> executionChecklist,
        List<String> controlPoints,
        Map<String, Object> metrics
) {
}
