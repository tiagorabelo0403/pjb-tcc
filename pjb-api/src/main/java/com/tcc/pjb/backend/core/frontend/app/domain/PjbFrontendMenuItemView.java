package com.tcc.pjb.backend.core.frontend.app.domain;

public record PjbFrontendMenuItemView(
        String code,
        String label,
        String path,
        String domain,
        String requiredAssuranceLevel,
        boolean requiresStepUp,
        boolean highlighted
) {

    public PjbFrontendMenuItemView {
        code = normalize(code);
        label = normalize(label);
        path = normalize(path);
        domain = normalize(domain);
        requiredAssuranceLevel = normalize(requiredAssuranceLevel);
    }

    public boolean hasDomain() {
        return !domain.isBlank();
    }

    public boolean isStepUpProtected() {
        return requiresStepUp || !requiredAssuranceLevel.isBlank();
    }

    public boolean isNavigable() {
        return !path.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
