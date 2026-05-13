package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.impl.NoopJudicialConnector;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;

class JudicialConnectorDataPlaneServiceTest {

    @Test
    void reportsTelemetryOverlayForTribunal() {
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
        JudicialConnectorTelemetryService telemetryService = Mockito.mock(JudicialConnectorTelemetryService.class);
        when(telemetryService.buildHealthReport(Duration.ofHours(24))).thenReturn(
                new JudicialConnectorTelemetryService.ConnectorTelemetryHealthReport(
                        Instant.now(),
                        Instant.now().minus(Duration.ofHours(24)),
                        1,
                        5,
                        List.of(new JudicialConnectorTelemetryService.ConnectorSystemHealth(
                                JudicialSystem.PJE,
                                5,
                                4,
                                1,
                                2,
                                2,
                                0.8d,
                                "DISPATCHED",
                                Instant.now(),
                                List.of("ok")
                        )),
                        List.of()
                )
        );
        JudicialConnectorDataPlaneService service = new JudicialConnectorDataPlaneService(
                properties,
                operationalProfileService,
                telemetryService
        );

        JudicialConnectorDataPlaneReport report = service.tribunalReport("TJCE", Duration.ofHours(24));

        assertThat(report.readySystems()).contains("PJE");
        assertThat(report.totalEvents()).isEqualTo(5L);
        assertThat(report.systems()).anyMatch(item -> item.system() == JudicialSystem.PJE && item.submissionReady());
        assertThat(report.toMap()).containsKeys("systems", "alerts");
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
