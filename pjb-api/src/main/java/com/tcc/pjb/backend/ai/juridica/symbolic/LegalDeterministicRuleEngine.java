package com.tcc.pjb.backend.ai.juridica.symbolic;

public interface LegalDeterministicRuleEngine {

    String engineCode();

    default boolean supports(String requestedCode) {
        return requestedCode != null && engineCode().equalsIgnoreCase(requestedCode.trim());
    }

    LegalSymbolicValidationOutcome evaluate(LegalSymbolicValidationContext context);
}
