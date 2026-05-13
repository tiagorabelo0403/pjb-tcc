package com.tcc.pjb.backend.integration.judicial.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JudicialConnectorSecurityPackServiceTest {

    @Test
    void resolvesMostSpecificSecurityPackForTribunal() {
        JudicialConnectorSecurityProperties properties = new JudicialConnectorSecurityProperties();
        properties.setEnvironmentName("prod");
        JudicialConnectorSecurityProperties.SecurityPack generic = new JudicialConnectorSecurityProperties.SecurityPack();
        generic.setSystem("PJE");
        generic.setTlsMode(JudicialConnectorTlsMode.TLS);
        generic.setProtocols(List.of("TLSv1.3"));
        generic.setOcspEnabled(true);
        generic.setMinimumRemainingValidity(Duration.ofDays(20));
        JudicialConnectorSecurityProperties.SecurityPack tribunalSpecific = new JudicialConnectorSecurityProperties.SecurityPack();
        tribunalSpecific.setSystem("PJE");
        tribunalSpecific.setTribunalCodigo("TJCE");
        tribunalSpecific.setEnvironmentName("prod");
        tribunalSpecific.setTlsMode(JudicialConnectorTlsMode.MTLS);
        tribunalSpecific.setKeyStoreRef("pkcs11-client");
        tribunalSpecific.setTrustStoreRef("tjce-ca");
        tribunalSpecific.setKeyAlias("tjce-a3");
        tribunalSpecific.setRequireClientCertificate(true);
        tribunalSpecific.setHostnameVerification(true);
        tribunalSpecific.setAllowedHosts(List.of("*.tjce.jus.br"));
        tribunalSpecific.setProtocols(List.of("TLSv1.3", "TLSv1.2"));
        tribunalSpecific.setRevocationMode(JudicialCertificateRevocationMode.HARD_FAIL);
        tribunalSpecific.setCrlEnabled(true);
        tribunalSpecific.setPreferCrl(true);
        properties.getPacks().put("generic", generic);
        properties.getPacks().put("tjce-pack", tribunalSpecific);

        JudicialIntegrationProperties integrationProperties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector connector = integrationProperties.getPje();
        connector.setEnabled(true);
        connector.setRequiresCertificate(true);
        connector.setBaseUrl("https://pje.tjce.jus.br/api");

        JudicialConnectorSecurityPackService service = new JudicialConnectorSecurityPackService(properties, integrationProperties);

        JudicialResolvedSecurityPack resolved = service.resolve(
                JudicialSystem.PJE,
                "TJCE",
                URI.create("https://pje.tjce.jus.br/api/protocolos"),
                connector,
                Map.of()
        );

        assertThat(resolved.packId()).isEqualTo("tjce-pack");
        assertThat(resolved.tlsMode()).isEqualTo(JudicialConnectorTlsMode.MTLS);
        assertThat(resolved.keyStoreRef()).isEqualTo("pkcs11-client");
        assertThat(resolved.trustStoreRef()).isEqualTo("tjce-ca");
        assertThat(resolved.keyAlias()).isEqualTo("tjce-a3");
        assertThat(resolved.requireClientCertificate()).isTrue();
        assertThat(resolved.revocationMode()).isEqualTo(JudicialCertificateRevocationMode.HARD_FAIL);
        assertThat(resolved.allowedHosts()).contains("*.tjce.jus.br");
    }
}
