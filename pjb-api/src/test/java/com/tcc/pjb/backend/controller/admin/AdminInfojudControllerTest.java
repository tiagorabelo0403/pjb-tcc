package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.integration.judicial.financeiro.InfojudApplicationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaSnapshot;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminInfojudControllerTest {

    private InfojudApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(InfojudApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminInfojudController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void consultar_deveExporResultado() throws Exception {
        when(applicationService.consultar(8L, "12345678901", "trail-1", false))
                .thenReturn(InfojudConsultaResult.success(15L, "PROTO-2", "OK"));

        mockMvc.perform(post("/api/v1/admin/infojud/consultas")
                        .param("processoId", "8")
                        .param("cpfCnpjConsultado", "12345678901")
                        .param("authzTrailId", "trail-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consultaId").value(15))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void snapshot_deveExporConsulta() throws Exception {
        when(applicationService.snapshot(15L)).thenReturn(new InfojudConsultaSnapshot(15L, "12345678901", "CONFIRMED", Instant.parse("2026-04-12T12:30:00Z")));

        mockMvc.perform(get("/api/v1/admin/infojud/consultas/15/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consultaId").value(15))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }
}
