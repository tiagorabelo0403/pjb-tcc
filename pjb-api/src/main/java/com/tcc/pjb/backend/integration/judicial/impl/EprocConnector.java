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
import jakarta.inject.Inject;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.integration.judicial.eproc", name = "enabled", havingValue = "true")
public class EprocConnector extends AbstractHttpJudicialConnector {

    @Inject
    public EprocConnector(JudicialIntegrationProperties props,
                         RestTemplateBuilder builder,
                         ObjectMapper objectMapper,
                         JudicialOAuthTokenService judicialOAuthTokenService,
                         JudicialConnectorTransport judicialConnectorTransport) {
        super(props, builder, objectMapper, judicialOAuthTokenService, judicialConnectorTransport, LoggerFactory.getLogger(EprocConnector.class));
    }

    EprocConnector(JudicialIntegrationProperties props,
                   RestTemplateBuilder builder,
                   ObjectMapper objectMapper,
                   JudicialOAuthTokenService judicialOAuthTokenService) {
        super(props, builder, objectMapper, judicialOAuthTokenService, LoggerFactory.getLogger(EprocConnector.class));
    }

    EprocConnector(JudicialIntegrationProperties props,
                   RestTemplateBuilder builder,
                   ObjectMapper objectMapper) {
        super(props, builder, objectMapper, null, LoggerFactory.getLogger(EprocConnector.class));
    }

    @Override
    public JudicialSystem system() {
        return JudicialSystem.EPROC;
    }

    @Override
    protected JudicialIntegrationProperties.Connector connectorConfig() {
        return props == null ? null : props.getEproc();
    }

    @Override
    protected String connectorLabel() {
        return "eproc";
    }

    @Override
    protected List<String> acceptedDocumentTypes() {
        return List.of("application/pdf", "image/png", "image/jpeg", "video/mp4", "application/zip");
    }

    @Override
    protected List<String> acceptedRamos() {
        return List.of("PREVIDENCIARIO", "TRIBUTARIO", "FAZENDA_PUBLICA", "CIVIL", "PENAL");
    }

    @Override
    protected List<String> acceptedScopes() {
        return List.of("PETICAO_INICIAL", "INTERMEDIARIA", "RECURSO");
    }

    @Override
    protected List<String> snapshotPathCandidates() {
        ArrayList<String> paths = new ArrayList<>(super.snapshotPathCandidates());
        paths.addAll(List.of("/eproc/api/processos/{numero}", "/eproc/processos/{numero}"));
        return List.copyOf(paths);
    }

    @Override
    protected List<String> eventsPathCandidates() {
        ArrayList<String> paths = new ArrayList<>(super.eventsPathCandidates());
        paths.addAll(List.of("/eproc/api/processos/{numero}/eventos", "/eproc/processos/{numero}/eventos"));
        return List.copyOf(paths);
    }

    @Override
    protected List<String> dryRunPathCandidates() {
        ArrayList<String> paths = new ArrayList<>(super.dryRunPathCandidates());
        paths.addAll(List.of("/eproc/api/protocolos/preflight", "/eproc/protocolos/preflight"));
        return List.copyOf(paths);
    }

    @Override
    protected List<String> submitPathCandidates() {
        ArrayList<String> paths = new ArrayList<>(super.submitPathCandidates());
        paths.addAll(List.of("/eproc/api/protocolos", "/eproc/protocolos"));
        return List.copyOf(paths);
    }

    @Override
    @Retry(name = "eproc")
    @CircuitBreaker(name = "eproc")
    public Optional<com.tcc.pjb.backend.integration.judicial.ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
        return super.fetchSnapshotByNumero(numeroUnificado);
    }

    @Override
    @Retry(name = "eproc")
    @CircuitBreaker(name = "eproc")
    public List<com.tcc.pjb.backend.integration.judicial.ExternalProcessEvent> fetchEvents(String numeroUnificado,
                                                                                           Instant since) {
        return super.fetchEvents(numeroUnificado, since);
    }

    @Override
    @Retry(name = "eproc")
    @CircuitBreaker(name = "eproc")
    public com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionResult submit(com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest request) {
        return super.submit(request);
    }
}
