package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.impl.NoopJudicialConnector;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class JudicialConnectorCryptographyProfileServiceTest {

    @Test
    void reportsCertificateHardenedConnectorWhenCertificateAndAuthAreSatisfied() {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://pje.tjce.test.local");
        cfg.setAuthRequired(true);
        cfg.setOauthTokenUrl("https://auth.test.local/oauth/token");
        cfg.setOauthClientId("client");
        cfg.setOauthClientSecret("secret");
        cfg.setRequiresCertificate(true);
        cfg.setCertificateAlias("A3-OAB-CE");
        cfg.setProductionReady(true);
        cfg.setSubmitPath("/api/protocolos");
        cfg.setHomologatedTribunals(List.of("TJCE"));
        properties.setPje(cfg);
        JudicialConnectorRegistry registry = new JudicialConnectorRegistry(List.of(
                connector(JudicialSystem.PJE, true, true, "https://pje.tjce.test.local", true),
                new NoopJudicialConnector()
        ));
        JudicialConnectorHomologationService homologationService = JudicialConnectorHomologationService.withoutPolicy(properties);
        JudicialConnectorReadinessService readinessService = new JudicialConnectorReadinessService(
                properties,
                homologationService,
                new JudicialOAuthTokenService(new RestTemplateBuilder(), new ObjectMapper())
        );
        JudicialConnectorOperationalProfileService operationalProfileService = new JudicialConnectorOperationalProfileService(
                registry,
                homologationService,
                readinessService
        );
        JudicialConnectorCryptographyProfileService service = new JudicialConnectorCryptographyProfileService(
                registry,
                properties,
                operationalProfileService
        );

        JudicialConnectorCryptographyReport report = service.tribunalReport("TJCE");

        assertThat(report.strongAuthenticationCount()).isGreaterThanOrEqualTo(1);
        assertThat(report.certificateReadyCount()).isGreaterThanOrEqualTo(1);
        assertThat(report.systems())
                .filteredOn(item -> item.system() == JudicialSystem.PJE)
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.cryptographyStatus()).startsWith("CERTIFICATE_HARDENED");
                    assertThat(item.certificateSatisfied()).isTrue();
                    assertThat(item.authenticationSatisfied()).isTrue();
                    assertThat(item.certificateAlias()).isEqualTo("A3-OAB-CE");
                    assertThat(item.authMode()).isEqualTo(JudicialConnectorAuthMode.OAUTH2_CLIENT_CREDENTIALS);
                });
    }

    private JudicialProcessConnector connector(JudicialSystem system,
                                               boolean enabled,
                                               boolean supportsProtocol,
                                               String baseUrl,
                                               boolean requiresCertificate) {
        return new JudicialProcessConnector() {
            @Override
            public JudicialSystem system() {
                return system;
            }

            @Override
            public java.util.Optional<ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.List<ExternalProcessEvent> fetchEvents(String numeroUnificado, Instant since) {
                return java.util.List.of();
            }

            @Override
            public JudicialSubmissionCapability capability() {
                return new JudicialSubmissionCapability(
                        system,
                        enabled,
                        supportsProtocol,
                        true,
                        true,
                        true,
                        false,
                        requiresCertificate,
                        true,
                        List.of("application/pdf"),
                        List.of("CIVIL"),
                        List.of("PETICAO_INICIAL"),
                        baseUrl
                );
            }
        };
    }
}
