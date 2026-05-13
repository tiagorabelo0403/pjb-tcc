package com.tcc.pjb.backend.ai.juridica.symbolic;

import java.util.List;
import java.util.Map;

public record LegalSymbolicValidationOutcome(
        String engineCode,
        String verdict,
        List<LegalSymbolicValidationIssue> issues,
        Map<String, Object> diagnostics
) {
    public static final String VERDICT_PASS = "PASS";
    public static final String VERDICT_WARN = "WARN";
    public static final String VERDICT_BLOCK = "BLOCK";

    public LegalSymbolicValidationOutcome {
        issues = issues == null ? List.of() : List.copyOf(issues);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }

    public static LegalSymbolicValidationOutcome of(String engineCode,
                                                    List<LegalSymbolicValidationIssue> issues,
                                                    Map<String, Object> diagnostics) {
        String verdict = VERDICT_PASS;
        if (issues != null && issues.stream().anyMatch(LegalSymbolicValidationIssue::isContradiction)) {
            verdict = VERDICT_BLOCK;
        } else if (issues != null && !issues.isEmpty()) {
            verdict = VERDICT_WARN;
        }
        return new LegalSymbolicValidationOutcome(engineCode, verdict, issues, diagnostics);
    }
}
