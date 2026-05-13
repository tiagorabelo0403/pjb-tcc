package com.tcc.pjb.backend.integration.judicial.impl;

import com.tcc.pjb.backend.integration.judicial.ExternalProcessEvent;
import com.tcc.pjb.backend.integration.judicial.ExternalProcessSnapshot;
import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(prefix = "pjb.integration.judicial.mp", name = "enabled", havingValue = "true")
public class MpConnector implements JudicialProcessConnector {

    private static final Logger log = LoggerFactory.getLogger(MpConnector.class);

    private final JudicialIntegrationProperties props;
    private final RestTemplate rest;

    public MpConnector(JudicialIntegrationProperties props, RestTemplateBuilder builder) {
        this.props = props;
        this.rest = builder.build();
    }

    @Override
    public JudicialSystem system() {
        return JudicialSystem.MP;
    }

    @Override
    @Retry(name = "mp")
    @CircuitBreaker(name = "mp")
    public Optional<ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
        String base = baseUrl();
        if (base == null) {
            return Optional.empty();
        }
        log.debug("[MP] fetchSnapshot numero={} baseUrl={}", numeroUnificado, base);
        return Optional.empty();
    }

    @Override
    @Retry(name = "mp")
    @CircuitBreaker(name = "mp")
    public List<ExternalProcessEvent> fetchEvents(String numeroUnificado, Instant since) {
        String base = baseUrl();
        if (base == null) {
            return List.of();
        }
        log.debug("[MP] fetchEvents numero={} since={} baseUrl={}", numeroUnificado, since, base);
        return List.of();
    }

    @Override
    public JudicialSubmissionCapability capability() {
        JudicialIntegrationProperties.Connector cfg = props == null ? null : props.getMp();
        return new JudicialSubmissionCapability(
                system(),
                cfg != null && cfg.isEnabled(),
                false,
                false,
                cfg == null || cfg.isSupportsSnapshotSync(),
                cfg == null || cfg.isSupportsEventSync(),
                false,
                false,
                cfg == null || cfg.isSupportsExternalMedia(),
                List.of("application/pdf"),
                List.of("PENAL", "MILITAR", "INFANCIA_JUVENTUDE"),
                List.of("CIENCIA", "PARECER", "MANIFESTACAO"),
                baseUrl()
        );
    }

    @Override
    public ProtocolSubmissionResult submit(ProtocolSubmissionRequest request) {
        return new ProtocolSubmissionResult(false, system(), null, "UNSUPPORTED", "Conector do MP opera sincronização e ciência, não protocolo originário.", Instant.now(), JudicialMapSupport.compact("requestId", request != null ? request.requestId() : null));
    }

    private String baseUrl() {
        if (props == null || props.getMp() == null) {
            return null;
        }
        String v = props.getMp().getBaseUrl();
        if (v == null || v.isBlank()) {
            return null;
        }
        return v.trim();
    }
}
