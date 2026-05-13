package com.tcc.pjb.backend.core.security.professional;

public record ProfessionalDocumentAccessDecision(
        boolean allowed,
        ProfessionalDocumentVisibilityScope scope,
        String reason
) {
}
