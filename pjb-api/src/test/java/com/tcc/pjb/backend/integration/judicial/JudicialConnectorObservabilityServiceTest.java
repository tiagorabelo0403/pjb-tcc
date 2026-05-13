package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JudicialConnectorObservabilityServiceTest {

    @Test
    void classifiesDegradedAndHealthySystemsFromControlAndDataPlane() {
        JudicialConnectorControlPlaneService controlPlaneService = mock(JudicialConnectorControlPlaneService.class);
        JudicialConnectorDataPlaneService dataPlaneService = mock(JudicialConnectorDataPlaneService.class);
        JudicialConnectorObservabilityService service = new JudicialConnectorObservabilityService(controlPlaneService, dataPlaneService);
        Instant now = Instant.parse("2026-03-10T12:00:00Z");

        JudicialConnectorControlPlaneReport controlPlaneReport = new JudicialConnectorControlPlaneReport(
                now,
                null,
                2,
                2,
                2,
                List.of("PJE", "ESAJ"),
                List.of("PJE", "ESAJ"),
                List.of(
                        new JudicialConnectorControlPlaneSystemReport(now, JudicialSystem.PJE, "TJCE", "PRODUCTION_READY", true, true, true, true, true, JudicialConnectorAuthMode.OAUTH2_CLIENT_CREDENTIALS, null, null, List.of(), List.of(), Map.of()),
                        new JudicialConnectorControlPlaneSystemReport(now, JudicialSystem.ESAJ, "TJSP", "PRODUCTION_READY_WITH_WARNINGS", true, true, true, true, true, JudicialConnectorAuthMode.API_KEY, null, null, List.of(), List.of("CONTROL_WARNING"), Map.of())
                ),
                List.of(),
                List.of(),
                Map.of()
        );

        JudicialConnectorDataPlaneReport dataPlaneReport = new JudicialConnectorDataPlaneReport(
                now,
                null,
                now.minus(Duration.ofHours(24)),
                9,
                List.of("PJE", "ESAJ"),
                List.of(
                        new JudicialConnectorDataPlaneSystemReport(now, JudicialSystem.PJE, "TJCE", "READY", true, true, 6, 6, 0, 4, 5, 1.0d, now.minusSeconds(60), null, List.of(), List.of(), Map.of("dominantStatus", "SUBMITTED")),
                        new JudicialConnectorDataPlaneSystemReport(now, JudicialSystem.ESAJ, "TJSP", "DEGRADED", true, true, 3, 1, 2, 0, 0, 0.3333d, now.minusSeconds(120), null, List.of(), List.of("DATA_PLANE_DEGRADED_SUCCESS_RATE"), Map.of("dominantStatus", "REJECTED"))
                ),
                List.of("DATA_ALERT"),
                Map.of()
        );

        when(controlPlaneService.nationalReport()).thenReturn(controlPlaneReport);
        when(dataPlaneService.nationalReport(Duration.ofHours(24))).thenReturn(dataPlaneReport);

        JudicialConnectorObservabilityReport report = service.nationalReport(Duration.ofHours(24));

        assertThat(report.healthySystems()).isEqualTo(1);
        assertThat(report.degradedSystems()).isEqualTo(1);
        assertThat(report.alerts()).contains("DATA_ALERT", "DATA_PLANE_DEGRADED_SUCCESS_RATE");
        assertThat(report.systems())
                .filteredOn(item -> item.system() == JudicialSystem.ESAJ)
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.observabilityStatus()).isEqualTo("DEGRADED");
                    assertThat(item.successRate()).isEqualTo(0.3333d);
                });
    }
}
