package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.integration.datajud.feed.DataJudApplicationService;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedHealthSnapshot;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedRunSummary;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminDataJudControllerTest {

    private DataJudApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(DataJudApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDataJudController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void run_deveExporResumo() throws Exception {
        when(applicationService.run("tjce")).thenReturn(new DataJudFeedRunSummary("TJCE", 4, 99L, true));

        mockMvc.perform(post("/api/v1/admin/datajud/tribunais/tjce/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tribunalCodigo").value("TJCE"))
                .andExpect(jsonPath("$.data.totalSent").value(4));
    }

    @Test
    void health_deveExporSnapshot() throws Exception {
        when(applicationService.health("tjce")).thenReturn(new DataJudFeedHealthSnapshot("TJCE", true, 10L, true));

        mockMvc.perform(get("/api/v1/admin/datajud/tribunais/tjce/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tribunalCodigo").value("TJCE"))
                .andExpect(jsonPath("$.data.healthy").value(true));
    }
}
