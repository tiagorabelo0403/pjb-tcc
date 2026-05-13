package com.tcc.pjb.backend.core.processo.painel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelTelemetriaConectorAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorDataPlaneReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorDataPlaneSystemReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorObservabilityReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorObservabilitySystemReport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.judicial.connectors.application.JudicialConnectorHubService;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorHubReport;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessoPainelTelemetriaConectorApplicationServiceTest {

    @Test
    void explicitaFallbackQuandoConectorDependeDeCache() {
        ProcessoUnificadoApplicationService unificadoService = mock(ProcessoUnificadoApplicationService.class);
        JudicialConnectorHubService hubService = mock(JudicialConnectorHubService.class);
        ProcessoPainelTelemetriaConectorApplicationService service = new ProcessoPainelTelemetriaConectorApplicationService(unificadoService, hubService);
        Instant now = Instant.parse("2026-03-21T18:00:00Z");

        when(unificadoService.detalhar(10L)).thenReturn(PainelTestData.unificado(10L, "TRABALHISTA", "TRT7"));
        when(hubService.tribunalReport("TRT7", java.time.Duration.ofHours(24))).thenReturn(new JudicialConnectorHubReport(
                now,
                "TRT7",
                null,
                new JudicialConnectorCommandCenterReport(
                        now,
                        "TRT7",
                        null,
                        null,
                        new JudicialConnectorDataPlaneReport(
                                now,
                                "TRT7",
                                now.minusSeconds(300),
                                10,
                                List.of("PJE"),
                                List.of(new JudicialConnectorDataPlaneSystemReport(
                                        now,
                                        JudicialSystem.PJE,
                                        "TRT7",
                                        "DEGRADED",
                                        true,
                                        false,
                                        10,
                                        6,
                                        1,
                                        3,
                                        2,
                                        0.8d,
                                        now.minusSeconds(30),
                                        null,
                                        List.of(),
                                        List.of("EVENT_SYNC_DELAY"),
                                        Map.of("baseUrl", "https://pje.trt7.jus.br")
                                )),
                                List.of(),
                                Map.of()
                        ),
                        null,
                        new JudicialConnectorObservabilityReport(
                                now,
                                "TRT7",
                                now.minusSeconds(300),
                                0,
                                1,
                                0,
                                List.of(new JudicialConnectorObservabilitySystemReport(
                                        now,
                                        JudicialSystem.PJE,
                                        "TRT7",
                                        "DEGRADED",
                                        true,
                                        true,
                                        true,
                                        false,
                                        true,
                                        10,
                                        0.8d,
                                        now.minusSeconds(30),
                                        List.of(),
                                        List.of("CACHE_ONLY"),
                                        Map.of()
                                )),
                                List.of(),
                                Map.of()
                        ),
                        null,
                        List.of(),
                        Map.of()
                ),
                null,
                List.of(),
                Map.of()
        ));

        ProcessoPainelTelemetriaConectorAggregate aggregate = service.detalhar(10L);

        assertThat(aggregate.conectores()).singleElement().satisfies(item -> {
            assertThat(item.connectorCode()).isEqualTo("PJE");
            assertThat(item.fallbackMode()).isEqualTo("ULTIMO_ESTADO_CACHE");
            assertThat(item.circuitMode()).isEqualTo("SEMI_ABERTO");
            assertThat(item.sourceEndpoints()).contains("https://pje.trt7.jus.br");
        });
        assertThat(aggregate.alertas()).contains("FALLBACK_EXPLICITO_ATIVO_POR_CACHE");
    }
}
