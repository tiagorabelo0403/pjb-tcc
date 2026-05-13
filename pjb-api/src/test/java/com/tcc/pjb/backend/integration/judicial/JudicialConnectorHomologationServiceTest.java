package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JudicialConnectorHomologationServiceTest {

    @Test
    void resolvesTribunalSpecificPathsAndHomologation() {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://pje.test.local");
        cfg.setProductionReady(true);
        cfg.setHomologatedTribunals(List.of("TJCE", "TJSP"));
        cfg.setTribunalSubmitPaths(Map.of("TJCE", "/tribunal/tjce/protocolos"));
        cfg.setTribunalDryRunPaths(Map.of("TJCE", "/tribunal/tjce/preflight"));
        properties.setPje(cfg);
        JudicialConnectorHomologationService service = JudicialConnectorHomologationService.withoutPolicy(properties);

        ProtocolSubmissionRequest request = new ProtocolSubmissionRequest(
                "REQ-H1",
                null,
                "Teste",
                "TJCE",
                null,
                null,
                "COMUM_ORDINARIO",
                "PROCEDIMENTO_COMUM_CIVEL",
                "CIVIL",
                "{}",
                null,
                null,
                null,
                false,
                Map.of()
        );
        JudicialSubmissionCapability capability = new JudicialSubmissionCapability(JudicialSystem.PJE, true, true, true, true, true, false, false, true, List.of(), List.of(), List.of(), "https://pje.test.local");

        JudicialConnectorHomologationReport report = service.analyze(JudicialSystem.PJE, capability, request);

        assertThat(report.tribunalHomologated()).isTrue();
        assertThat(report.submitHomologated()).isTrue();
        assertThat(report.effectiveSubmitPath()).isEqualTo("/tribunal/tjce/protocolos");
        assertThat(report.effectiveDryRunPath()).isEqualTo("/tribunal/tjce/preflight");
        assertThat(report.blockers()).isEmpty();
    }

    @Test
    void blocksWhenTribunalIsExplicitlyForbidden() {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://esaj.test.local");
        cfg.setProductionReady(true);
        cfg.setBlockedTribunals(List.of("TJSP"));
        properties.setEsaj(cfg);
        JudicialConnectorHomologationService service = JudicialConnectorHomologationService.withoutPolicy(properties);

        ProtocolSubmissionRequest request = new ProtocolSubmissionRequest(
                "REQ-H2",
                null,
                "Teste",
                "TJSP",
                null,
                null,
                "COMUM_ORDINARIO",
                "PROCEDIMENTO_COMUM_CIVEL",
                "CIVIL",
                "{}",
                null,
                null,
                null,
                false,
                Map.of()
        );
        JudicialSubmissionCapability capability = new JudicialSubmissionCapability(JudicialSystem.ESAJ, true, true, true, true, true, false, false, true, List.of(), List.of(), List.of(), "https://esaj.test.local");

        JudicialConnectorHomologationReport report = service.analyze(JudicialSystem.ESAJ, capability, request);

        assertThat(report.tribunalBlocked()).isTrue();
        assertThat(report.blockers()).contains("CONNECTOR_TRIBUNAL_BLOCKED");
    }
}
