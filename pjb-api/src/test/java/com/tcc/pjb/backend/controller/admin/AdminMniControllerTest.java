package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.integration.mni.MniApplicationService;
import com.tcc.pjb.backend.integration.mni.domain.MniReprocessamentoSummary;
import com.tcc.pjb.backend.integration.mni.domain.MniRemessaStatusSnapshot;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminMniControllerTest {

    private MniApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(MniApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMniController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void reprocessar_deveResponderResumo() throws Exception {
        when(applicationService.reprocessar(12)).thenReturn(new MniReprocessamentoSummary(3, 1, 0));

        mockMvc.perform(post("/api/v1/admin/mni/remessas/reprocess").param("limit", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processadas").value(3));
    }

    @Test
    void remessaStatus_deveExporSnapshot() throws Exception {
        when(applicationService.remessaStatus(7L)).thenReturn(new MniRemessaStatusSnapshot(7L, "CONFIRMED", "PR-1", Instant.parse("2026-04-11T12:00:00Z"), Instant.parse("2026-04-11T12:05:00Z")));

        mockMvc.perform(get("/api/v1/admin/mni/remessas/7/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remessaId").value(7))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }
}
