package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public record JudicialResolvedSecurityBinding(
        String bindingId,
        JudicialSystem system,
        String tribunalCodigo,
        String environmentName,
        boolean enabled,
        JudicialConnectorTlsMode tlsMode,
        String keyStoreRef,
        String trustStoreRef,
        String keyAlias,
        String certificateAlias,
        boolean requireClientCertificate,
        boolean hostnameVerification,
        Duration connectTimeout,
        Duration readTimeout,
        List<String> protocols,
        List<String> cipherSuites,
        List<String> allowedHosts,
        Map<String, Object> metadata
) {
    public JudicialResolvedSecurityBinding {
        protocols = protocols == null ? List.of() : List.copyOf(protocols);
        cipherSuites = cipherSuites == null ? List.of() : List.copyOf(cipherSuites);
        allowedHosts = allowedHosts == null ? List.of() : List.copyOf(allowedHosts);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public boolean mutualTls() {
        return tlsMode == JudicialConnectorTlsMode.MTLS;
    }

    public boolean transportSecurityEnabled() {
        return tlsMode != JudicialConnectorTlsMode.DISABLED;
    }
}
