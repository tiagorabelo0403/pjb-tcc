package com.tcc.pjb.backend.core.comunicacao.institucional;

import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import java.util.Locale;

public final class InstitutionalScopeResolutionSupport {

    private InstitutionalScopeResolutionSupport() {
    }

    public static InstitutionalOrganizationScope fallback(InstitutionalOrganizationScope scope) {
        return scope == null ? InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL : scope;
    }

    public static String code(InstitutionalOrganizationScope scope) {
        return fallback(scope).name();
    }

    public static boolean matchesFilter(String candidateScope, String requestedScope) {
        if (requestedScope == null || requestedScope.isBlank()) {
            return true;
        }
        if (candidateScope == null || candidateScope.isBlank()) {
            return false;
        }
        return normalize(candidateScope).equals(normalize(requestedScope));
    }

    public static InstitutionalOrganizationScope parseOrDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL;
        }
        String normalized = normalize(raw);
        for (InstitutionalOrganizationScope item : InstitutionalOrganizationScope.values()) {
            if (item.name().equals(normalized)) {
                return item;
            }
        }
        return InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL;
    }

    private static String normalize(String value) {
        return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
