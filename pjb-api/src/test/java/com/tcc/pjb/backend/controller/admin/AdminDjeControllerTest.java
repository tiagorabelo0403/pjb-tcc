package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.dje.DjeApplicationService;
import com.tcc.pjb.backend.core.dje.domain.DjeLifecycleExecutionSummary;
import com.tcc.pjb.backend.core.dje.domain.DjePublicationMetricsView;
import com.tcc.pjb.backend.core.dje.domain.DjeTimelineEntry;
import com.tcc.pjb.backend.core.dje.domain.DjeTimelineView;
import com.tcc.pjb.backend.core.dje.domain.DjeTribunalHealthView;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminDjeControllerTest {

    private DjeApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(DjeApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDjeController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void metrics_deveExporResumoDePublicacoes() throws Exception {
        when(applicationService.metrics()).thenReturn(new DjePublicationMetricsView(2, 3, 4, 1));

        mockMvc.perform(get("/api/v1/admin/dje/metrics/publication"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.pending").value(2))
                .andExpect(jsonPath("$.data.published").value(4));
    }

    @Test
    void lifecycleRun_deveExecutarRodadaManual() throws Exception {
        when(applicationService.lifecycleRun(LocalDate.of(2026, 4, 11), 15))
                .thenReturn(new DjeLifecycleExecutionSummary(3, 2));

        mockMvc.perform(post("/api/v1/admin/dje/lifecycle/run")
                        .param("hoje", "2026-04-11")
                        .param("batchSize", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("lifecycle do DJe executado"))
                .andExpect(jsonPath("$.data.publicadasConsolidadas").value(3))
                .andExpect(jsonPath("$.data.partesNotificadas").value(2));
    }

    @Test
    void timeline_deveExporEventosDaPublicacao() throws Exception {
        when(applicationService.timeline(9L)).thenReturn(new DjeTimelineView(
                9L,
                List.of(
                        new DjeTimelineEntry("CRIADO", Instant.parse("2026-04-11T12:00:00Z"), "SENTENCA"),
                        new DjeTimelineEntry("PUBLICADO", Instant.parse("2026-04-12T12:00:00Z"), "PUBLICADO"))));

        mockMvc.perform(get("/api/v1/admin/dje/publicacoes/9/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.djeId").value(9))
                .andExpect(jsonPath("$.data.entries[1].evento").value("PUBLICADO"));
    }

    @Test
    void tribunalHealth_deveExporSaudeDoTribunal() throws Exception {
        when(applicationService.tribunalHealth("tjce")).thenReturn(new DjeTribunalHealthView("TJCE", "OK", "total=5"));

        mockMvc.perform(get("/api/v1/admin/dje/tribunais/tjce/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reference").value("TJCE"))
                .andExpect(jsonPath("$.data.status").value("OK"))
                .andExpect(jsonPath("$.data.summary").value("total=5"));
    }
}
