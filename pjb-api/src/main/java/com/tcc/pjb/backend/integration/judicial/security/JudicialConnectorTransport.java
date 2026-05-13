package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.net.URI;

public interface JudicialConnectorTransport {
    JudicialSecureHttpResponse exchange(JudicialSystem system,
                                        String tribunalCodigo,
                                        URI targetUri,
                                        JudicialIntegrationProperties.Connector connectorConfig,
                                        JudicialSecureHttpRequest request);
}
