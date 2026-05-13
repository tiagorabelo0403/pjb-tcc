package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JudicialConnectorReadinessServiceTest {

    @Test
    void blocksConnectorWhenAuthAndCertificateEvidenceAreMissing() {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://pje.test.local");
        cfg.setAuthRequired(true);
        cfg.setRequiresCertificate(true);
        properties.setPje(cfg);
        JudicialConnectorReadinessService service = new JudicialConnectorReadinessService(properties, JudicialConnectorHomologationService.withoutPolicy(properties), new JudicialOAuthTokenService(new org.springframework.boot.web.client.RestTemplateBuilder(), new com.fasterxml.jackson.databind.ObjectMapper()));

        JudicialSubmissionCapability capability = new JudicialSubmissionCapability(
                JudicialSystem.PJE,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                java.util.List.of("application/pdf"),
                java.util.List.of("CIVIL"),
                java.util.List.of("PETICAO_INICIAL"),
                "https://pje.test.local"
        );

        ProtocolSubmissionRequest request = new ProtocolSubmissionRequest(
                "REQ-1",
                "0000001-00.2026.8.06.0001",
                "Petição inicial",
                "TJCE",
                "TJCE-1VC",
                "1ª Vara Cível",
                "COMUM_ORDINARIO",
                "PROCEDIMENTO_COMUM_CIVEL",
                "CIVIL",
                "{}",
                "HASH",
                1L,
                1L,
                false,
                Map.of()
        );

        JudicialConnectorReadinessReport report = service.analyze(JudicialSystem.PJE, capability, request);

        assertThat(report.readyForSubmission()).isFalse();
        assertThat(report.blockers()).contains("CONNECTOR_AUTH_MISSING", "CONNECTOR_CERTIFICATE_EVIDENCE_MISSING");
    }

    @Test
    void acceptsConnectorWhenOperationalEvidenceIsPresentInConfigAndRequest() {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://esaj.test.local");
        cfg.setAuthRequired(true);
        cfg.setApiKey("secret-key");
        cfg.setRequiresCertificate(true);
        cfg.setCertificateAlias("A3-OAB");
        cfg.setSubmitPath("/custom/protocolos");
        cfg.setProductionReady(true);
        cfg.setHomologatedTribunals(java.util.List.of("TJSP"));
        properties.setEsaj(cfg);
        JudicialConnectorReadinessService service = new JudicialConnectorReadinessService(properties, JudicialConnectorHomologationService.withoutPolicy(properties), new JudicialOAuthTokenService(new org.springframework.boot.web.client.RestTemplateBuilder(), new com.fasterxml.jackson.databind.ObjectMapper()));

        JudicialSubmissionCapability capability = new JudicialSubmissionCapability(
                JudicialSystem.ESAJ,
                true,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                java.util.List.of("application/pdf"),
                java.util.List.of("CIVIL"),
                java.util.List.of("PETICAO_INICIAL"),
                "https://esaj.test.local"
        );

        ProtocolSubmissionRequest request = new ProtocolSubmissionRequest(
                "REQ-2",
                "0000002-00.2026.8.26.0100",
                "Ação de obrigação de fazer",
                "TJSP",
                "TJSP-1VC",
                "1ª Vara Cível",
                "COMUM_ORDINARIO",
                "PROCEDIMENTO_COMUM_CIVEL",
                "CIVIL",
                "{}",
                "HASH2",
                2L,
                2L,
                false,
                Map.of("govbrStepUp", true)
        );

        JudicialConnectorReadinessReport report = service.analyze(JudicialSystem.ESAJ, capability, request);

        assertThat(report.readyForSubmission()).isTrue();
        assertThat(report.blockers()).isEmpty();
        assertThat(report.metadata()).containsEntry("authMode", "API_KEY");
    }
}
