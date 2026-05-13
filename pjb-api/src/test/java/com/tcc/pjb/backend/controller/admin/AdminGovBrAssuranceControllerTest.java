package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.security.GovBrAssuranceApplicationService;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceResult;
import com.tcc.pjb.backend.core.security.domain.GovBrAssuranceStatusView;
import com.tcc.pjb.backend.core.security.domain.GovBrStepUpHealthView;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminGovBrAssuranceControllerTest {

    private GovBrAssuranceApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(GovBrAssuranceApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminGovBrAssuranceController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void evaluate_deveExporResultadoDePolicy() throws Exception {
        when(applicationService.evaluate("prata", true)).thenReturn(new GovBrAssuranceResult("prata", "ouro", false, true));

        mockMvc.perform(get("/api/v1/admin/govbr/assurance/evaluate").param("nivelAtual", "prata").param("atoSensivel", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nivelAtual").value("prata"))
                .andExpect(jsonPath("$.data.nivelRequerido").value("ouro"))
                .andExpect(jsonPath("$.data.exigeStepUp").value(true));
    }

    @Test
    void status_deveExporStatusAtual() throws Exception {
        when(applicationService.status("ouro", false)).thenReturn(new GovBrAssuranceStatusView("ouro", "ALLOWED", "requerido=prata"));

        mockMvc.perform(get("/api/v1/admin/govbr/assurance/status").param("nivelAtual", "ouro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reference").value("ouro"))
                .andExpect(jsonPath("$.data.status").value("ALLOWED"));
    }

    @Test
    void stepUpHealth_deveExporSaudeDoStepUp() throws Exception {
        when(applicationService.stepUpHealth("bronze", true)).thenReturn(new GovBrStepUpHealthView("bronze", "STEP_UP_REQUIRED", Instant.parse("2026-04-11T10:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/govbr/assurance/step-up/health").param("nivelAtual", "bronze").param("atoSensivel", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referencia").value("bronze"))
                .andExpect(jsonPath("$.data.status").value("STEP_UP_REQUIRED"));
    }
}
