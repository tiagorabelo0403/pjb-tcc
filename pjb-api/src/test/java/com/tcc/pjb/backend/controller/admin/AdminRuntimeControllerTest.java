package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.platform.runtime.PjbRuntimeApplicationService;
import com.tcc.pjb.backend.platform.runtime.domain.PjbReadAfterWritePolicyView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeDrainView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeHealthView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeSizingView;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminRuntimeControllerTest {

    private PjbRuntimeApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(PjbRuntimeApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRuntimeController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void sizing_deveExporFootprint() throws Exception {
        when(applicationService.sizing()).thenReturn(new PjbRuntimeSizingView(8, 2048L, "api"));

        mockMvc.perform(get("/api/v1/admin/runtime/sizing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableProcessors").value(8))
                .andExpect(jsonPath("$.data.componentRole").value("api"));
    }

    @Test
    void beginDrain_deveExporComandoAdministrativo() throws Exception {
        when(applicationService.beginDrain("ops")).thenReturn(new PjbRuntimeDrainView(true, false, Instant.parse("2026-04-11T12:00:00Z"), 1500L, "ops", 20000L));

        mockMvc.perform(post("/api/v1/admin/runtime/drain/begin").param("reason", "ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("runtime drain iniciado"))
                .andExpect(jsonPath("$.data.draining").value(true));
    }

    @Test
    void health_deveExporSaudeDoRuntime() throws Exception {
        when(applicationService.health()).thenReturn(new PjbRuntimeHealthView("UP", true, false, "score=0 draining=false trend=steady"));

        mockMvc.perform(get("/api/v1/admin/runtime/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.ready").value(true));
    }

    @Test
    void rawPolicy_deveExporJanelaConfigurada() throws Exception {
        when(applicationService.rawPolicy()).thenReturn(new PjbReadAfterWritePolicyView(2000L, true, "PRIMARY_STRICT"));

        mockMvc.perform(get("/api/v1/admin/runtime/raw/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.windowMillis").value(2000))
                .andExpect(jsonPath("$.data.forceRoute").value("PRIMARY_STRICT"));
    }
}
