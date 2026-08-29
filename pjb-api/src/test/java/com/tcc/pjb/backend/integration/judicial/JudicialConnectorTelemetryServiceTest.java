package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorTelemetry;
import com.tcc.pjb.backend.model.repository.JudicialConnectorTelemetryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class JudicialConnectorTelemetryServiceTest {

    @Test
    void buildsHealthReportFromRecentTelemetry() {
        JudicialConnectorTelemetryRepository repository = Mockito.mock(JudicialConnectorTelemetryRepository.class);
        JudicialConnectorTelemetry accepted = new JudicialConnectorTelemetry();
        accepted.setConnectorSystem(JudicialSystem.PJE);
        accepted.setEventType("SUBMISSION_RESULT");
        accepted.setStatus("SUBMITTED");
        accepted.setAccepted(true);
        accepted.setMessage("ok");
        accepted.setCreatedAt(Instant.now().minusSeconds(60));

        JudicialConnectorTelemetry rejected = new JudicialConnectorTelemetry();
        rejected.setConnectorSystem(JudicialSystem.PJE);
        rejected.setEventType("SUBMISSION_RESULT");
        rejected.setStatus("CONNECTOR_ERROR");
        rejected.setAccepted(false);
        rejected.setMessage("erro");
        rejected.setCreatedAt(Instant.now().minusSeconds(30));

        JudicialConnectorTelemetry snapshot = new JudicialConnectorTelemetry();
        snapshot.setConnectorSystem(JudicialSystem.PJE);
        snapshot.setEventType("SNAPSHOT_SYNC");
        snapshot.setStatus("SNAPSHOT_FOUND");
        snapshot.setAccepted(true);
        snapshot.setCreatedAt(Instant.now().minusSeconds(10));

        when(repository.findAllByCreatedAtAfterOrderByCreatedAtDesc(any())).thenReturn(List.of(snapshot, rejected, accepted));

        JudicialConnectorTelemetryService service = new JudicialConnectorTelemetryService(repository, new ObjectMapper());
        var report = service.buildHealthReport(Duration.ofHours(6));

        assertThat(report.totalEvents()).isEqualTo(3);
        assertThat(report.systems()).hasSize(1);
        assertThat(report.systems().get(0).acceptedSubmissions()).isEqualTo(1);
        assertThat(report.systems().get(0).rejectedSubmissions()).isEqualTo(1);
        assertThat(report.systems().get(0).snapshotHits()).isEqualTo(1);
    }

    @Test
    void recordsSubmissionResult() {
        JudicialConnectorTelemetryRepository repository = Mockito.mock(JudicialConnectorTelemetryRepository.class);
        JudicialConnectorTelemetryService service = new JudicialConnectorTelemetryService(repository, new ObjectMapper());
        ProtocolSubmissionRequest request = new ProtocolSubmissionRequest(
                "REQ-1",
                "0000001-00.2026.8.06.0001",
                "Obrigação de fazer",
                "TJCE",
                "TJCE-CIVEL-CE-CAP",
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
        ProtocolSubmissionResult result = new ProtocolSubmissionResult(true, JudicialSystem.PJE, "PJE-1", "SUBMITTED", "ok", Instant.now(), Map.of());

        service.recordSubmissionResult(null, request, result);

        ArgumentCaptor<JudicialConnectorTelemetry> captor = ArgumentCaptor.forClass(JudicialConnectorTelemetry.class);
        verify(repository).save(captor.capture());
        JudicialConnectorTelemetry saved = captor.getValue();
        assertThat(saved.getTribunalCodigo()).isEqualTo("TJCE");
        assertThat(saved.getUnidadeJudiciariaCodigo()).isEqualTo("TJCE-CIVEL-CE-CAP");
        assertThat(saved.getConnectorSystem()).isEqualTo(JudicialSystem.PJE);
        assertThat(saved.getEventType()).isEqualTo("SUBMISSION_RESULT");
        assertThat(saved.getStatus()).isEqualTo("SUBMITTED");
        assertThat(saved.getAccepted()).isTrue();
        assertThat(saved.getProtocolReference()).isEqualTo("PJE-1");
    }
}
