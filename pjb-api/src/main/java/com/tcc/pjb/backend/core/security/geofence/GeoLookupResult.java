package com.tcc.pjb.backend.core.security.geofence;

public record GeoLookupResult(
        String countryIso,
        String subdivisionIso,
        boolean anonymousProxyOuHosting,
        boolean disponivel
) {
    public static GeoLookupResult indisponivel() {
        return new GeoLookupResult(null, null, false, false);
    }
}
