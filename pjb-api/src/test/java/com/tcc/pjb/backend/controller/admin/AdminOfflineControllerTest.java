package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.offline.OfflineApplicationService;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleMetricsView;
import com.tcc.pjb.backend.service.offline.domain.OfflineConflictTimelineEntry;
import com.tcc.pjb.backend.service.offline.domain.OfflineConflictTimelineResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleGovernanceStatusView;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminOfflineControllerTest {

    private OfflineApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(OfflineApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOfflineController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void metrics_deveExporResumoDoBundle() throws Exception {
        when(applicationService.metrics("PWA-1")).thenReturn(new OfflineBundleMetricsView(1L, 5, 1, Instant.parse("2026-04-11T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/offline/bundles/PWA-1/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bundleId").value(1))
                .andExpect(jsonPath("$.data.actionsCount").value(5));
    }

    @Test
    void governanceStatus_deveExporStatusGovernado() throws Exception {
        when(applicationService.governanceStatus("PWA-2")).thenReturn(new OfflineBundleGovernanceStatusView("PWA-2", "OK", "replay seguro"));

        mockMvc.perform(get("/api/v1/admin/offline/bundles/PWA-2/governance/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reference").value("PWA-2"))
                .andExpect(jsonPath("$.data.status").value("OK"));
    }

    @Test
    void conflictTimeline_deveExporEventos() throws Exception {
        when(applicationService.conflictTimeline("PWA-3")).thenReturn(new OfflineConflictTimelineResult(
                "PWA-3",
                List.of(new OfflineConflictTimelineEntry("CONFLITO", Instant.parse("2026-04-11T10:00:00Z"), "review"))));

        mockMvc.perform(get("/api/v1/admin/offline/bundles/PWA-3/conflict/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bundleToken").value("PWA-3"))
                .andExpect(jsonPath("$.data.entries[0].evento").value("CONFLITO"));
    }
}
