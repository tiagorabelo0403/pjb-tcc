package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.criminal.custodia.CustodiaApplicationService;
import com.tcc.pjb.backend.core.criminal.custodia.domain.AudienciaCustodiaResult;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaPrazoConsultaResult;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminCustodiaControllerTest {

    private CustodiaApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(CustodiaApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCustodiaController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void registrarPrisao_deveExporResultado() throws Exception {
        when(applicationService.registrarPrisao(4L, "João", "123", null)).thenReturn(new AudienciaCustodiaResult(7L, Instant.parse("2026-04-12T12:00:00Z")));

        mockMvc.perform(post("/api/v1/admin/custodia/registrar-prisao")
                        .param("processoId", "4")
                        .param("presoNome", "João")
                        .param("presoCpf", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.custodiaId").value(7));
    }

    @Test
    void prazo_deveExporConsulta() throws Exception {
        when(applicationService.prazo(7L)).thenReturn(new CustodiaPrazoConsultaResult(7L, Instant.parse("2026-04-11T12:00:00Z"), Instant.parse("2026-04-12T12:00:00Z"), Duration.ofHours(4), false));

        mockMvc.perform(get("/api/v1/admin/custodia/7/prazo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.custodiaId").value(7))
                .andExpect(jsonPath("$.data.vencido").value(false));
    }
}
