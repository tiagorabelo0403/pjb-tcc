package com.tcc.pjb.backend.integration.judicial.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;

class JudicialConnectorCryptographicContextServiceTest {

    @Test
    void resolvesMostSpecificBindingForTribunalAndEnvironment() {
        JudicialConnectorSecurityProperties properties = new JudicialConnectorSecurityProperties();
        properties.setEnvironmentName("prod");
        JudicialConnectorSecurityProperties.ConnectorBinding generic = new JudicialConnectorSecurityProperties.ConnectorBinding();
        generic.setSystem("PJE");
        generic.setTlsMode(JudicialConnectorTlsMode.TLS);
        generic.setProtocols(List.of("TLSv1.3"));
        JudicialConnectorSecurityProperties.ConnectorBinding specific = new JudicialConnectorSecurityProperties.ConnectorBinding();
        specific.setSystem("PJE");
        specific.setTribunalCodigo("TJCE");
        specific.setEnvironmentName("prod");
        specific.setTlsMode(JudicialConnectorTlsMode.MTLS);
        specific.setKeyStoreRef("client-cert");
        specific.setTrustStoreRef("judicial-ca");
        specific.setRequireClientCertificate(true);
        specific.setKeyAlias("tribunal-a3");
        properties.getBindings().put("generic", generic);
        properties.getBindings().put("specific", specific);
        JudicialPropertySecretResolver secretResolver = new JudicialPropertySecretResolver(new MockEnvironment(), new DefaultResourceLoader());
        JudicialIntegrationProperties integrationProperties = new JudicialIntegrationProperties();
        JudicialConnectorSecurityPackService packService = new JudicialConnectorSecurityPackService(properties, integrationProperties);
        JudicialConnectorCryptographicContextService service = new JudicialConnectorCryptographicContextService(
                properties,
                packService,
                new JudicialKeyStoreLoader(properties, new JudicialPkcs11ProviderRegistry(properties, secretResolver), secretResolver, new DefaultResourceLoader()),
                mock(JudicialConnectorLowLevelSecurityAuditService.class)
        );
        JudicialIntegrationProperties.Connector connector = new JudicialIntegrationProperties.Connector();
        connector.setRequiresCertificate(true);

        JudicialResolvedSecurityBinding binding = service.resolveBinding(
                JudicialSystem.PJE,
                "TJCE",
                URI.create("https://pje.tjce.jus.br/api/protocolos"),
                connector,
                Map.of()
        );

        assertThat(binding.bindingId()).isEqualTo("specific");
        assertThat(binding.tlsMode()).isEqualTo(JudicialConnectorTlsMode.MTLS);
        assertThat(binding.requireClientCertificate()).isTrue();
        assertThat(binding.keyStoreRef()).isEqualTo("client-cert");
        assertThat(binding.trustStoreRef()).isEqualTo("judicial-ca");
        assertThat(binding.keyAlias()).isEqualTo("tribunal-a3");
    }

    @Test
    void forbidsCleartextHttpWhenTlsIsForced() {
        JudicialConnectorSecurityProperties properties = new JudicialConnectorSecurityProperties();
        JudicialConnectorSecurityProperties.ConnectorBinding binding = new JudicialConnectorSecurityProperties.ConnectorBinding();
        binding.setSystem("ESAJ");
        binding.setTlsMode(JudicialConnectorTlsMode.TLS);
        properties.getBindings().put("esaj-default", binding);
        JudicialPropertySecretResolver secretResolver = new JudicialPropertySecretResolver(new MockEnvironment(), new DefaultResourceLoader());
        JudicialIntegrationProperties integrationProperties = new JudicialIntegrationProperties();
        JudicialConnectorSecurityPackService packService = new JudicialConnectorSecurityPackService(properties, integrationProperties);
        JudicialConnectorCryptographicContextService service = new JudicialConnectorCryptographicContextService(
                properties,
                packService,
                new JudicialKeyStoreLoader(properties, new JudicialPkcs11ProviderRegistry(properties, secretResolver), secretResolver, new DefaultResourceLoader()),
                mock(JudicialConnectorLowLevelSecurityAuditService.class)
        );
        JudicialIntegrationProperties.Connector connector = new JudicialIntegrationProperties.Connector();

        assertThatThrownBy(() -> service.resolve(
                JudicialSystem.ESAJ,
                "TJSP",
                URI.create("http://esaj.local/protocolos"),
                connector,
                Map.of("requestId", "REQ-1")
        )).isInstanceOf(JudicialConnectorCryptographicException.class);
    }
}
