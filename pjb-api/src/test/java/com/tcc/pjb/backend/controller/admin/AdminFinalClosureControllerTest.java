package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.quality.finalclosure.application.PjbFinalClosureApplicationService;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureBlockerView;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureReadinessView;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureSummary;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureSweepView;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminFinalClosureControllerTest {

    private PjbFinalClosureApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(PjbFinalClosureApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminFinalClosureController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void summary_deveExporEstadoGlobal() throws Exception {
        when(applicationService.summary()).thenReturn(new PjbFinalClosureSummary(
                false,
                true,
                false,
                true,
                false,
                false,
                false,
                23,
                1,
                21,
                1,
                22,
                40,
                120,
                20,
                List.of("gate pendente"),
                Instant.parse("2026-04-12T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/admin/final-closure/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallReady").value(false))
                .andExpect(jsonPath("$.data.totalMacroblocks").value(23));
    }

    @Test
    void blockers_deveExporBloqueadoresConsolidados() throws Exception {
        when(applicationService.blockers()).thenReturn(List.of(new PjbFinalClosureBlockerView("roadmap", "roadmap.status", "ALTO", "roadmap", "macrobloco parcial")));

        mockMvc.perform(get("/api/v1/admin/final-closure/blockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scope").value("roadmap"));
    }

    @Test
    void readiness_deveExporDimensoesDeProntidao() throws Exception {
        when(applicationService.readiness()).thenReturn(List.of(new PjbFinalClosureReadinessView("end-to-end-validation", "BLOCKED", "sem prova global", List.of("rodar build"))));

        mockMvc.perform(get("/api/v1/admin/final-closure/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dimension").value("end-to-end-validation"))
                .andExpect(jsonPath("$.data[0].status").value("BLOCKED"));
    }

    @Test
    void sweep_deveExporContagemDaSuperficie() throws Exception {
        when(applicationService.sweep()).thenReturn(new PjbFinalClosureSweepView(40, 120, 20, 1, List.of("AdminRuntimeController"), List.of("PjbRuntimeApplicationService"), Instant.parse("2026-04-12T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/final-closure/sweep"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminControllers").value(40))
                .andExpect(jsonPath("$.data.partialMacroblocksWithoutAdminSurface").value(1));
    }
}
