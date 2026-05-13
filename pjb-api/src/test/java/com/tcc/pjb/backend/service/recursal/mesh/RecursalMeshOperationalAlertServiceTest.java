package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardBucket;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshDashboardResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshIndexDriftReport;

class RecursalMeshOperationalAlertServiceTest {

    @Test
    void shouldBuildOperationalAlertsFromDriftFailuresAndGargalos() {
        RecursalMeshDashboardService dashboardService = mock(RecursalMeshDashboardService.class);
        RecursalMeshIndexDriftService driftService = mock(RecursalMeshIndexDriftService.class);
        RecursalMeshOperationalTelemetryService telemetryService = new RecursalMeshOperationalTelemetryService(new SimpleMeterRegistry());
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider = (ObjectProvider<RecursalMeshOperationalTelemetryService>) mock(ObjectProvider.class);
        when(telemetryProvider.getIfAvailable()).thenReturn(telemetryService);
        when(dashboardService.dashboard(org.mockito.ArgumentMatchers.any())).thenReturn(new RecursalMeshDashboardResponse(
                "RELATIONAL_FALLBACK",
                25,
                6,
                7,
                3,
                2,
                1,
                List.of(new RecursalMeshDashboardBucket("JULGAMENTO_COLEGIADO", 7L)),
                List.of(new RecursalMeshDashboardBucket("STJ", 7L)),
                List.of(new RecursalMeshDashboardBucket("RELATOR", 5L)),
                List.of(new RecursalMeshDashboardBucket("STJ", 10L)),
                List.of(new RecursalMeshDashboardBucket("RELATOR", 9L)),
                List.of(new RecursalMeshDashboardBucket("CRITICO_PARTES", 3L)),
                List.of(new RecursalMeshDashboardBucket("TEMA-1102 — Tema repetitivo 1102", 6L))
        ));
        when(driftService.assess(org.mockito.ArgumentMatchers.any())).thenReturn(new RecursalMeshIndexDriftReport(
                "ASSESSED",
                "pjb-recursal-mesh",
                100,
                92,
                50,
                4,
                2,
                1,
                1,
                "HIGH",
                Instant.parse("2026-04-05T16:00:00Z")
        ));
        telemetryService.recordNotificationDelivery("outbox", false);
        telemetryService.recordRetryExhausted("index", "reindex-batch-save");

        RecursalMeshOperationalAlertService service = new RecursalMeshOperationalAlertService(dashboardService, driftService, telemetryProvider);
        var report = service.report(500, 5);

        assertThat(report.alertas()).isNotEmpty();
        assertThat(report.falhasNotificacaoPorCanal()).first().extracting(RecursalMeshDashboardBucket::key).isEqualTo("outbox");
        assertThat(report.retryExaustoPorAlvo()).first().extracting(RecursalMeshDashboardBucket::key).isEqualTo("index:reindex-batch-save");
        assertThat(report.alertas()).extracting(alert -> alert.code()).contains("INDEX_DRIFT", "NOTIFICATION_FAILURE_CHANNEL", "RETRY_EXHAUSTED_TARGET");
    }
}
