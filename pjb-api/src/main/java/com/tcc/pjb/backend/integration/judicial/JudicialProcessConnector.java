package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JudicialProcessConnector {

    JudicialSystem system();

    Optional<ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado);

    List<ExternalProcessEvent> fetchEvents(String numeroUnificado, Instant since);

    default JudicialSubmissionCapability capability() {
        return new JudicialSubmissionCapability(
                system(),
                true,
                false,
                false,
                true,
                true,
                false,
                false,
                false,
                List.of("application/pdf"),
                List.of(),
                List.of(),
                null
        );
    }

    default ProtocolSubmissionResult submit(ProtocolSubmissionRequest request) {
        return new ProtocolSubmissionResult(
                false,
                system(),
                null,
                "UNSUPPORTED",
                "Conector sem suporte de protocolo para o sistema " + system().name(),
                Instant.now(),
                JudicialMapSupport.compact("requestId", request != null ? request.requestId() : null)
        );
    }
}
