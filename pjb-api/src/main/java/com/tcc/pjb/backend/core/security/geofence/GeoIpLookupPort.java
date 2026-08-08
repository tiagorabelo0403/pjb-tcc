package com.tcc.pjb.backend.core.security.geofence;

public interface GeoIpLookupPort {
    GeoLookupResult lookup(String ip);
}
