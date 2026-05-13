package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.impl.NoopJudicialConnector;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class JudicialConnectorGovernanceServiceTest {

    @Test
    void reportsConnectorPolicyConflictsAndTribunalLandscape() {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://pje.test.local");
        cfg.setProductionReady(true);
        cfg.setSubmitPath("/api/protocolos");
        cfg.setHomologatedTribunals(List.of("TJCE", "TJRN"));
        cfg.setBlockedTribunals(List.of("TJRN"));
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
        JudicialConnectorGovernanceService service = new JudicialConnectorGovernanceService(
                registry,
                properties,
                operationalProfileService
        );

        JudicialConnectorGovernanceReport report = service.report();
        JudicialConnectorTribunalLandscapeReport tribunalReport = service.reportForTribunal("TJCE");

        assertThat(report.blockers()).contains("CONNECTOR_TRIBUNAL_POLICY_CONFLICT");
        assertThat(report.toMap()).containsKey("connectors");
        assertThat(tribunalReport.readySystems()).contains("PJE");
        assertThat(tribunalReport.productionReadySystems()).contains("PJE");
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
