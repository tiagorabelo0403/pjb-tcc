package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProcessSyncServiceTest {

    @Test
    void syncSnapshotPublishesSnapshotEvent() {
        JudicialConnectorRegistry registry = Mockito.mock(JudicialConnectorRegistry.class);
        ProcessEventNormalizer normalizer = new ProcessEventNormalizer();
        JudicialConnectorTelemetryService telemetryService = Mockito.mock(JudicialConnectorTelemetryService.class);
        OutboxPublisher outbox = Mockito.mock(OutboxPublisher.class);
        JudicialProcessConnector connector = new JudicialProcessConnector() {
            @Override
            public JudicialSystem system() {
                return JudicialSystem.PJE;
            }

            @Override
            public Optional<ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
                return Optional.of(new ExternalProcessSnapshot(JudicialSystem.PJE, numeroUnificado, "PROCEDIMENTO_COMUM_CIVEL", "Obrigação de fazer", NivelSigilo.PUBLICO, Instant.now(), Map.of("raw", true)));
            }

            @Override
            public List<ExternalProcessEvent> fetchEvents(String numeroUnificado, Instant since) {
                return List.of();
            }
        };
        when(registry.get(JudicialSystem.PJE)).thenReturn(connector);
        ProcessSyncService service = new ProcessSyncService(registry, normalizer, outbox, telemetryService);

        var snapshot = service.syncSnapshot(JudicialSystem.PJE, "0000001-00.2026.8.06.0001");

        assertThat(snapshot).isPresent();
        verify(outbox).enqueue(any(), eq(ProcessSyncService.OUTBOX_SNAPSHOT_TYPE), any(), any(), any(), eq("Processo"), eq("0000001-00.2026.8.06.0001"));
    }
}
