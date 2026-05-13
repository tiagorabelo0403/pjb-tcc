package com.tcc.pjb.backend.core.frontend.publicaccess;

import java.util.Set;

public record PjbPublicPortalCapabilityProfile(Set<PjbPublicPortalCapability> enabledCapabilities) {
    public PjbPublicPortalCapabilityProfile {
        enabledCapabilities = enabledCapabilities == null ? Set.of() : Set.copyOf(enabledCapabilities);
    }

    public boolean supports(PjbPublicPortalCapability capability) {
        return capability != null && enabledCapabilities.contains(capability);
    }

    public boolean replacementReady() {
        return enabledCapabilities.containsAll(Set.of(
                PjbPublicPortalCapability.PROCESS_NUMBER_SEARCH,
                PjbPublicPortalCapability.DOCUMENT_VERIFICATION,
                PjbPublicPortalCapability.PUBLIC_TIMELINE,
                PjbPublicPortalCapability.PROCEDURAL_PUSH,
                PjbPublicPortalCapability.PLAIN_LANGUAGE_GUIDE
        ));
    }
}
