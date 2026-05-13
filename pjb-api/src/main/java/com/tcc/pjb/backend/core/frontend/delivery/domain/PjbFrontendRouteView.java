package com.tcc.pjb.backend.core.frontend.delivery.domain;

public record PjbFrontendRouteView(
        String method,
        String path,
        String controller,
        String packageName,
        String domain,
        boolean adminSurface,
        boolean uiSurface
) {

    public PjbFrontendRouteView {
        method = normalize(method).toUpperCase();
        path = normalize(path);
        controller = normalize(controller);
        packageName = normalize(packageName);
        domain = normalize(domain);
    }

    public String routeKey() {
        return method + " " + path;
    }

    public boolean hasDomain() {
        return !domain.isBlank();
    }

    public boolean belongsToController(String controllerName) {
        return controller.equalsIgnoreCase(normalize(controllerName));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
