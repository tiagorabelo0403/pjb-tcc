package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.quality.roadmap.application.PjbRoadmapClosureApplicationService;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapBlockingView;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapClosureSummary;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapMacroblockView;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapQualityGateView;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminRoadmapClosureControllerTest {

    private PjbRoadmapClosureApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(PjbRoadmapClosureApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRoadmapClosureController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void summary_deveExporPlacares() throws Exception {
        when(applicationService.summary()).thenReturn(new PjbRoadmapClosureSummary(23, 1, 21, 1, 4, 20, 22, Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/roadmap/closure/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(23))
                .andExpect(jsonPath("$.data.blockingCount").value(22));
    }

    @Test
    void macroblocks_deveExporMacroblocos() throws Exception {
        when(applicationService.macroblocks()).thenReturn(List.of(new PjbRoadmapMacroblockView(11, "Parte 1", "Extração pjb-core / multi-module inicial", "Parcial", "MATERIALIZADO", true)));

        mockMvc.perform(get("/api/v1/admin/roadmap/closure/macroblocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].number").value(11))
                .andExpect(jsonPath("$.data[0].status").value("Parcial"));
    }

    @Test
    void quality_deveExporGateConsolidado() throws Exception {
        when(applicationService.quality()).thenReturn(new PjbRoadmapQualityGateView(true, true, true, false, 3, List.of("extrair pjb-core"), List.of("PrazoProcessualNacionalService")));

        mockMvc.perform(get("/api/v1/admin/roadmap/closure/quality"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buildApproved").value(true))
                .andExpect(jsonPath("$.data.modularizationReady").value(false));
    }

    @Test
    void blockers_deveExporBloqueadoresConsolidados() throws Exception {
        when(applicationService.blockers()).thenReturn(List.of(new PjbRoadmapBlockingView("modularization", "root.without.modules", "ALTO", "POM sem modules")));

        mockMvc.perform(get("/api/v1/admin/roadmap/closure/blockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scope").value("modularization"));
    }
}
