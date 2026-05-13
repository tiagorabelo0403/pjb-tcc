package com.tcc.pjb.backend.integration.judicial.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.JudicialOAuthTokenService;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorTransport;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.integration.judicial.pdpj", name = "enabled", havingValue = "true")
public class PdpjConnector extends AbstractHttpJudicialConnector {

    public PdpjConnector(JudicialIntegrationProperties props,
                         RestTemplateBuilder builder,
                         ObjectMapper objectMapper,
                         JudicialOAuthTokenService judicialOAuthTokenService,
                         JudicialConnectorTransport judicialConnectorTransport) {
        super(props, builder, objectMapper, judicialOAuthTokenService, judicialConnectorTransport, LoggerFactory.getLogger(PdpjConnector.class));
    }

    public PdpjConnector(JudicialIntegrationProperties props,
                         RestTemplateBuilder builder,
                         ObjectMapper objectMapper,
                         JudicialOAuthTokenService judicialOAuthTokenService) {
        super(props, builder, objectMapper, judicialOAuthTokenService, LoggerFactory.getLogger(PdpjConnector.class));
    }

    public PdpjConnector(JudicialIntegrationProperties props,
                         RestTemplateBuilder builder,
                         ObjectMapper objectMapper) {
        super(props, builder, objectMapper, null, LoggerFactory.getLogger(PdpjConnector.class));
    }

    @Override
    public JudicialSystem system() {
        return JudicialSystem.PDPJ;
    }

    @Override
    protected JudicialIntegrationProperties.Connector connectorConfig() {
        return props == null ? null : props.getPdpj();
    }

    @Override
    protected String connectorLabel() {
        return "PDPJ";
    }

    @Override
    protected List<String> acceptedDocumentTypes() {
        return List.of("application/pdf", "application/xml", "application/zip", "image/png", "video/mp4");
    }

    @Override
    protected List<String> acceptedRamos() {
        return List.of("CIVIL", "PENAL", "ELEITORAL", "MILITAR", "TRABALHISTA", "PREVIDENCIARIO", "TRIBUTARIO", "FAZENDA_PUBLICA", "INFANCIA_JUVENTUDE", "AGRARIO", "AMBIENTAL");
    }

    @Override
    protected List<String> acceptedScopes() {
        return List.of("PETICAO_INICIAL", "INTERMEDIARIA", "RECURSO", "CUMPRIMENTO", "EXECUCAO");
    }

    @Override
    protected List<String> snapshotPathCandidates() {
        ArrayList<String> paths = new ArrayList<>(super.snapshotPathCandidates());
        paths.addAll(List.of("/pdpj/api/processos/{numero}", "/pdpj/processos/{numero}"));
        return List.copyOf(paths);
    }

    @Override
    protected List<String> eventsPathCandidates() {
        ArrayList<String> paths = new ArrayList<>(super.eventsPathCandidates());
        paths.addAll(List.of("/pdpj/api/processos/{numero}/eventos", "/pdpj/processos/{numero}/eventos"));
        return List.copyOf(paths);
    }

    @Override
    protected List<String> dryRunPathCandidates() {
        ArrayList<String> paths = new ArrayList<>(super.dryRunPathCandidates());
        paths.addAll(List.of("/pdpj/api/protocolos/preflight", "/pdpj/protocolos/preflight"));
        return List.copyOf(paths);
    }

    @Override
    protected List<String> submitPathCandidates() {
        ArrayList<String> paths = new ArrayList<>(super.submitPathCandidates());
        paths.addAll(List.of("/pdpj/api/protocolos", "/pdpj/protocolos"));
        return List.copyOf(paths);
    }

    @Override
    @Retry(name = "pdpj")
    @CircuitBreaker(name = "pdpj")
    public Optional<com.tcc.pjb.backend.integration.judicial.ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
        return super.fetchSnapshotByNumero(numeroUnificado);
    }

    @Override
    @Retry(name = "pdpj")
    @CircuitBreaker(name = "pdpj")
    public List<com.tcc.pjb.backend.integration.judicial.ExternalProcessEvent> fetchEvents(String numeroUnificado,
                                                                                           Instant since) {
        return super.fetchEvents(numeroUnificado, since);
    }

    @Override
    @Retry(name = "pdpj")
    @CircuitBreaker(name = "pdpj")
    public com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionResult submit(com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest request) {
        return super.submit(request);
    }
}
