package com.tcc.pjb.backend.integration.judicial.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

class HttpJudicialConnectorContractTest {

    private EsajConnector connector;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://esaj.test.local");
        properties.setEsaj(cfg);
        connector = new EsajConnector(properties, new RestTemplateBuilder(), new ObjectMapper());
        server = MockRestServiceServer.bindTo(connector.rest).ignoreExpectOrder(true).build();
    }

    @Test
    void submitsRealProtocolAgainstFirstOperationalEndpoint() {
        server.expect(requestTo("https://esaj.test.local/api/protocolos"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"status":"SUBMITTED","protocolReference":"ESAJ-2026-0001","message":"aceito"}
                        """, MediaType.APPLICATION_JSON));

        var result = connector.submit(new ProtocolSubmissionRequest(
                "REQ-1",
                "0000001-00.2026.8.26.0100",
                "Ação de obrigação de fazer",
                "TJSP",
                "TJSP-CIVEL-SP-CAP",
                "1ª Vara Cível",
                "COMUM_ORDINARIO",
                "PROCEDIMENTO_COMUM_CIVEL",
                "CIVIL",
                "{}",
                "HASH-ABC",
                10L,
                10L,
                false,
                Map.of()
        ));

        assertThat(result.accepted()).isTrue();
        assertThat(result.protocolReference()).isEqualTo("ESAJ-2026-0001");
        assertThat(result.status()).isEqualTo("SUBMITTED");
    }

    @Test
    void fetchesSnapshotAndEventsFromConventionalEndpoints() {
        String numero = "0000001-00.2026.8.26.0100";
        Instant since = Instant.parse("2026-03-09T00:00:00Z");
        server.expect(requestTo("https://esaj.test.local/api/processos/0000001-00.2026.8.26.0100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"numeroUnificado":"0000001-00.2026.8.26.0100","classeProcessual":"PROCEDIMENTO_COMUM_CIVEL","assunto":"Obrigação de fazer","nivelSigilo":"PUBLICO"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://esaj.test.local/api/processos/0000001-00.2026.8.26.0100/eventos?since=2026-03-09T00:00:00Z"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"EV-1","tipo":"JUNTADA","descricao":"Juntada de petição","ocorridoEm":"2026-03-09T11:00:00Z"}]
                        """, MediaType.APPLICATION_JSON));

        var snapshot = connector.fetchSnapshotByNumero(numero);
        var events = connector.fetchEvents(numero, since);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.orElseThrow().classeProcessual()).isEqualTo("PROCEDIMENTO_COMUM_CIVEL");
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().externalId()).isEqualTo("EV-1");
        assertThat(events.getFirst().type()).isEqualTo("JUNTADA");
    }
    @Test
    void honorsConfiguredAuthAndCustomPaths() {
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://esaj.test.local");
        cfg.setApiKey("secret-token");
        cfg.setApiKeyHeader("X-ESAJ-Key");
        cfg.setSubmitPath("/custom/protocolos");
        cfg.setSnapshotPath("/custom/processos/{numero}");
        cfg.setEventsPath("/custom/processos/{numero}/eventos");
        properties.setEsaj(cfg);
        connector = new EsajConnector(properties, new RestTemplateBuilder(), new ObjectMapper());
        server = MockRestServiceServer.bindTo(connector.rest).ignoreExpectOrder(true).build();

        server.expect(requestTo("https://esaj.test.local/custom/protocolos"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-ESAJ-Key", "secret-token"))
                .andRespond(withSuccess("""
                        {"status":"SUBMITTED","protocolReference":"ESAJ-2026-9000","message":"aceito"}
                        """, MediaType.APPLICATION_JSON));

        var result = connector.submit(new ProtocolSubmissionRequest(
                "REQ-9",
                "0000009-00.2026.8.26.0100",
                "Ação declaratória",
                "TJSP",
                "TJSP-CIVEL-SP-CAP",
                "1ª Vara Cível",
                "COMUM_ORDINARIO",
                "PROCEDIMENTO_COMUM_CIVEL",
                "CIVIL",
                "{}",
                "HASH-XYZ",
                9L,
                9L,
                false,
                Map.of()
        ));

        assertThat(result.accepted()).isTrue();
        assertThat(result.protocolReference()).isEqualTo("ESAJ-2026-9000");
    }

}
