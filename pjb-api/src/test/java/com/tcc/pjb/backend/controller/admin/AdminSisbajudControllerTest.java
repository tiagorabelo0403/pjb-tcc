package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.integration.judicial.financeiro.SisbajudApplicationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoSnapshot;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminSisbajudControllerTest {

    private SisbajudApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(SisbajudApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminSisbajudController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void bloquear_deveExporResultado() throws Exception {
        when(applicationService.bloquear(8L, "123", BigDecimal.TEN, "OF-1", "trail-1", false))
                .thenReturn(SisbajudBloqueioResult.success(12L, "PROTO-1", "OK"));

        mockMvc.perform(post("/api/v1/admin/sisbajud/operacoes/bloqueio")
                        .param("processoId", "8")
                        .param("cpfDevedor", "123")
                        .param("valor", "10")
                        .param("numeroOficio", "OF-1")
                        .param("authzTrailId", "trail-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operacaoId").value(12))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void snapshot_deveExporOperacao() throws Exception {
        when(applicationService.snapshot(12L)).thenReturn(new SisbajudOperacaoSnapshot(12L, 8L, "CONFIRMED", BigDecimal.TEN, "PROTO-1"));

        mockMvc.perform(get("/api/v1/admin/sisbajud/operacoes/12/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operacaoId").value(12))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }
}
