package com.tcc.pjb.backend.ai.juridica.symbolic;

import java.util.List;
import java.util.Map;

public record LegalSymbolicValidationExecution(
        String status,
        List<LegalSymbolicValidationOutcome> outcomes,
        List<String> contradictions,
        List<String> missingEvidence,
        Map<String, Object> diagnostics
) {
    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_WARN = "WARN";
    public static final String STATUS_BLOCK = "BLOCK";

    public LegalSymbolicValidationExecution {
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
        contradictions = contradictions == null ? List.of() : List.copyOf(contradictions);
        missingEvidence = missingEvidence == null ? List.of() : List.copyOf(missingEvidence);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }
}
