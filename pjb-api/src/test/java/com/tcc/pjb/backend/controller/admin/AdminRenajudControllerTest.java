package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.integration.judicial.financeiro.RenajudApplicationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoSnapshot;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminRenajudControllerTest {

    private RenajudApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(RenajudApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRenajudController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void restringir_deveExporResultado() throws Exception {
        when(applicationService.restringir(8L, "ABC1234", null, "RESTRICAO", "trail-1"))
                .thenReturn(RenajudRestricaoResult.success(12L, "PROTO-1", "OK"));

        mockMvc.perform(post("/api/v1/admin/renajud/restricoes")
                        .param("processoId", "8")
                        .param("placa", "ABC1234")
                        .param("tipo", "RESTRICAO")
                        .param("authzTrailId", "trail-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restricaoId").value(12))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void snapshot_deveExporRestricao() throws Exception {
        when(applicationService.snapshot(12L)).thenReturn(new RenajudRestricaoSnapshot(12L, "ABC1234", "CONFIRMED", Instant.parse("2026-04-12T12:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/renajud/restricoes/12/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restricaoId").value(12))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }
}
