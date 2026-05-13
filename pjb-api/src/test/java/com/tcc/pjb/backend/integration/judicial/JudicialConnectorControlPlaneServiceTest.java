package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.impl.NoopJudicialConnector;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class JudicialConnectorControlPlaneServiceTest {

    @Test
    void reportsNationalAndTribunalControlPlane() {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://pje.test.local");
        cfg.setProductionReady(true);
        cfg.setSubmitPath("/api/protocolos");
        cfg.setSnapshotPath("/api/processos/snapshot");
        cfg.setEventsPath("/api/processos/eventos");
        cfg.setHomologatedTribunals(List.of("TJCE"));
        properties.setPje(cfg);
        JudicialConnectorRegistry registry = new JudicialConnectorRegistry(List.of(
                connector(JudicialSystem.PJE, true, true, "https://pje.test.local"),
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
        JudicialConnectorGovernanceService governanceService = new JudicialConnectorGovernanceService(
                registry,
                properties,
                operationalProfileService
        );
        JudicialConnectorControlPlaneService service = new JudicialConnectorControlPlaneService(
                registry,
                properties,
                governanceService,
                operationalProfileService
        );

        JudicialConnectorControlPlaneReport national = service.nationalReport();
        JudicialConnectorControlPlaneReport tribunal = service.tribunalReport("TJCE");

        assertThat(national.productionReadySystems()).contains("PJE");
        assertThat(tribunal.tribunalReadySystems()).contains("PJE");
        assertThat(tribunal.systems()).extracting(item -> item.system().name()).contains("PJE");
        assertThat(tribunal.toMap()).containsKeys("systems", "metadata");
    }

    private JudicialProcessConnector connector(JudicialSystem system,
                                               boolean enabled,
                                               boolean supportsProtocol,
                                               String baseUrl) {
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
            public java.util.List<ExternalProcessEvent> fetchEvents(String numeroUnificado, java.time.Instant since) {
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
                        false,
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
