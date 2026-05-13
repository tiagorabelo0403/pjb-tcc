package com.tcc.pjb.backend.integration.judicial.impl;

import com.tcc.pjb.backend.integration.judicial.ExternalProcessEvent;
import com.tcc.pjb.backend.integration.judicial.ExternalProcessSnapshot;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NoopJudicialConnector implements JudicialProcessConnector {

    @Override
    public JudicialSystem system() {
        return JudicialSystem.OUTRO;
    }

    @Override
    public Optional<ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
        return Optional.empty();
    }

    @Override
    public List<ExternalProcessEvent> fetchEvents(String numeroUnificado, Instant since) {
        return List.of();
    }

    @Override
    public JudicialSubmissionCapability capability() {
        return new JudicialSubmissionCapability(system(), true, false, false, false, false, false, false, false, List.of("application/pdf"), List.of(), List.of(), null);
    }

    @Override
    public ProtocolSubmissionResult submit(ProtocolSubmissionRequest request) {
        return new ProtocolSubmissionResult(false, system(), null, "NO_CONNECTOR", "Nenhum conector judicial operacional foi selecionado.", Instant.now(), JudicialMapSupport.compact("requestId", request != null ? request.requestId() : null));
    }
}
