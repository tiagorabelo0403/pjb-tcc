package com.tcc.pjb.backend.ai.juridica.symbolic;

import java.util.Map;

public record LegalSymbolicValidationIssue(
        String engineCode,
        String type,
        String severity,
        String message,
        Map<String, Object> evidence
) {
    public static final String TYPE_MISSING_EVIDENCE = "MISSING_EVIDENCE";
    public static final String TYPE_CONTRADICTION = "CONTRADICTION";
    public static final String TYPE_NOTICE = "NOTICE";
    public static final String SEVERITY_WARN = "WARN";
    public static final String SEVERITY_BLOCK = "BLOCK";
    public static final String SEVERITY_INFO = "INFO";

    public LegalSymbolicValidationIssue {
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }

    public static LegalSymbolicValidationIssue missingEvidence(String engineCode, String message, Map<String, Object> evidence) {
        return new LegalSymbolicValidationIssue(engineCode, TYPE_MISSING_EVIDENCE, SEVERITY_WARN, message, evidence);
    }

    public static LegalSymbolicValidationIssue contradiction(String engineCode, String message, Map<String, Object> evidence) {
        return new LegalSymbolicValidationIssue(engineCode, TYPE_CONTRADICTION, SEVERITY_BLOCK, message, evidence);
    }

    public static LegalSymbolicValidationIssue notice(String engineCode, String message, Map<String, Object> evidence) {
        return new LegalSymbolicValidationIssue(engineCode, TYPE_NOTICE, SEVERITY_INFO, message, evidence);
    }

    public boolean isContradiction() {
        return TYPE_CONTRADICTION.equalsIgnoreCase(type);
    }

    public boolean isMissingEvidence() {
        return TYPE_MISSING_EVIDENCE.equalsIgnoreCase(type);
    }
}
