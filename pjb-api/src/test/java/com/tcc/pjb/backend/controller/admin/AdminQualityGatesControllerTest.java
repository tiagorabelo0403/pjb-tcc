package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.quality.gates.application.PjbQualityGateReadinessApplicationService;
import com.tcc.pjb.backend.core.quality.gates.domain.PjbArchitectureQualityView;
import com.tcc.pjb.backend.core.quality.gates.domain.PjbQualityBlockerView;
import com.tcc.pjb.backend.core.quality.gates.domain.PjbQualityGateSummary;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.model.dto.governance.TestQualityMatrixResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminQualityGatesControllerTest {

    private PjbQualityGateReadinessApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(PjbQualityGateReadinessApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminQualityGatesController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void summary_deveExporResumo() throws Exception {
        when(applicationService.summary()).thenReturn(new PjbQualityGateSummary(true, true, true, true, true, true, true, 0, Instant.parse("2026-04-12T12:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/quality-gates/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buildApproved").value(true))
                .andExpect(jsonPath("$.data.blockerCount").value(0));
    }

    @Test
    void architecture_deveExporLeituraArquitetural() throws Exception {
        when(applicationService.architecture()).thenReturn(new PjbArchitectureQualityView(true, true, true, 2, true, List.of()));

        mockMvc.perform(get("/api/v1/admin/quality-gates/architecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.matchingTests").value(2));
    }

    @Test
    void build_deveExporGateEstrutural() throws Exception {
        when(applicationService.buildGate()).thenReturn(new BuildGateEvaluationResponse(false, true, true, true, true, true, false, 2, List.of("build bloqueado"), List.of()));

        mockMvc.perform(get("/api/v1/admin/quality-gates/build"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approved").value(false));
    }

    @Test
    void blockers_deveExporBloqueadores() throws Exception {
        when(applicationService.blockers()).thenReturn(List.of(new PjbQualityBlockerView("mutation", "mutacao.blocked", "ALTO", "threshold ausente")));

        mockMvc.perform(get("/api/v1/admin/quality-gates/blockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scope").value("mutation"));
    }

    @Test
    void matrix_deveExporMatriz() throws Exception {
        when(applicationService.matrix()).thenReturn(new TestQualityMatrixResponse(10, 5, 10, 5, 1, List.of("PrazoProcessualNacionalService"), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/admin/quality-gates/matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalControllers").value(10));
    }
}
