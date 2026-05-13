package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public record JudicialConnectorCryptographicContext(
        JudicialResolvedSecurityBinding binding,
        SSLContext sslContext,
        SSLParameters sslParameters,
        String selectedKeyAlias,
        boolean hardwareBacked,
        Map<String, Object> metadata
) {
    public JudicialConnectorCryptographicContext {
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public boolean mutualTls() {
        return binding != null && binding.mutualTls();
    }

    public boolean transportSecurityEnabled() {
        return binding != null && binding.transportSecurityEnabled();
    }
}
