package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.impl.NoopJudicialConnector;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class JudicialConnectorOperationalProfileServiceTest {

    @Test
    void composesReadinessHomologationAndAuthModeIntoSingleProfile() {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://esaj.test.local");
        cfg.setAuthRequired(true);
        cfg.setApiKey("api-key-1");
        cfg.setProductionReady(true);
        cfg.setSubmitPath("/custom/protocolos");
        cfg.setHomologatedTribunals(List.of("TJSP"));
        properties.setEsaj(cfg);
        JudicialConnectorRegistry registry = new JudicialConnectorRegistry(List.of(
                connector(JudicialSystem.ESAJ, true, true, true, "https://esaj.test.local"),
                new NoopJudicialConnector()
        ));
        JudicialConnectorHomologationService homologationService = JudicialConnectorHomologationService.withoutPolicy(properties);
        JudicialConnectorReadinessService readinessService = new JudicialConnectorReadinessService(
                properties,
                homologationService,
                new JudicialOAuthTokenService(new RestTemplateBuilder(), new ObjectMapper())
        );
        JudicialConnectorOperationalProfileService service = new JudicialConnectorOperationalProfileService(
                registry,
                homologationService,
                readinessService
        );

        ProtocolSubmissionRequest request = new ProtocolSubmissionRequest(
                "REQ-ESAJ-1",
                null,
                "Petição inicial",
                "TJSP",
                null,
                null,
                null,
                null,
                null,
                "{}",
                null,
                null,
                null,
                false,
                Map.of()
        );

        JudicialConnectorOperationalProfileReport report = service.analyze(JudicialSystem.ESAJ, request);

        assertThat(report.readyForTribunalSubmission()).isTrue();
        assertThat(report.readyForProduction()).isTrue();
        assertThat(report.authMode()).isEqualTo(JudicialConnectorAuthMode.API_KEY);
        assertThat(report.metadata()).containsEntry("submitPath", "/custom/protocolos");
        assertThat(report.readiness().readyForSubmission()).isTrue();
        assertThat(report.homologation().tribunalHomologated()).isTrue();
    }

    private JudicialProcessConnector connector(JudicialSystem system,
                                               boolean enabled,
                                               boolean supportsProtocol,
                                               boolean supportsDryRun,
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
                        supportsDryRun,
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
