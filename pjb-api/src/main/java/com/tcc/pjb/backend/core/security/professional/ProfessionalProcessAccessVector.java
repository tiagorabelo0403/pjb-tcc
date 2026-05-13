package com.tcc.pjb.backend.core.security.professional;

import java.util.List;

public record ProfessionalProcessAccessVector(
        boolean allowed,
        String panelMode,
        ProfessionalActorClass actorClass,
        ProfessionalAccessBasis primaryBasis,
        List<ProfessionalAccessBasis> allBases,
        List<ProfessionalCapability> capabilities,
        List<ProfessionalDocumentVisibilityScope> allowedScopes,
        List<ProfessionalDocumentVisibilityScope> restrictedScopes,
        boolean requiresStepUp,
        boolean represented,
        boolean publicOnly,
        String reason
) {
    public boolean hasCapability(ProfessionalCapability capability) {
        return capability != null && capabilities != null && capabilities.contains(capability);
    }

    public boolean allowsScope(ProfessionalDocumentVisibilityScope scope) {
        return scope != null && allowedScopes != null && allowedScopes.contains(scope);
    }
}
