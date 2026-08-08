package com.tcc.pjb.backend.core.security.geofence;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pjb.security.geofence")
public record GeofenceProperties(
        String databasePath,
        List<String> asnVpnConhecidos,
        @DefaultValue("true") boolean enforceRealEmProd
) {

    public GeofenceProperties {
        asnVpnConhecidos = asnVpnConhecidos == null ? List.of() : List.copyOf(asnVpnConhecidos);
    }
}
