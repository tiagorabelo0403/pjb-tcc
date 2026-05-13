package com.tcc.pjb.backend.service.processual.calculo;

public record CalculoJudicialApiRouteContext(
        String apiFamily,
        String routeStatus,
        String operation,
        String domain,
        boolean frontendReady,
        boolean legacy
) {

    public CalculoJudicialApiRouteContext {
        apiFamily = normalize(apiFamily);
        routeStatus = normalize(routeStatus);
        operation = normalize(operation);
        domain = normalize(domain);
    }

    public boolean isActive() {
        return !routeStatus.isBlank() && !"disabled".equalsIgnoreCase(routeStatus);
    }

    public boolean hasDomain() {
        return !domain.isBlank();
    }

    public boolean isLegacyGap() {
        return legacy && !frontendReady;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
