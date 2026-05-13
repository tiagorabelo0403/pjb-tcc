package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.judicial.sobrestamento.SobrestamentoApplicationService;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaConsultaResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaHealthResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaResult;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminSobrestamentoControllerTest {

    private SobrestamentoApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(SobrestamentoApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminSobrestamentoController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void run_deveExecutarSobrestamentoManual() throws Exception {
        when(applicationService.sobrestar("TEMA_7")).thenReturn(new SobrestamentoTemaResult("TEMA_7", 3, Instant.parse("2026-04-12T15:20:00Z")));

        mockMvc.perform(post("/api/v1/admin/sobrestamento/TEMA_7/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigoTema").value("TEMA_7"))
                .andExpect(jsonPath("$.data.totalAfetado").value(3));
    }

    @Test
    void consulta_deveExporConsultaDoTema() throws Exception {
        when(applicationService.consulta("TEMA_8")).thenReturn(new SobrestamentoTemaConsultaResult("TEMA_8", 22L, "EM_ANALISE"));

        mockMvc.perform(get("/api/v1/admin/sobrestamento/TEMA_8/consulta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processosSobrestados").value(22));
    }

    @Test
    void health_deveExporHealthDoTema() throws Exception {
        when(applicationService.health("TEMA_9")).thenReturn(new SobrestamentoTemaHealthResult("TEMA_9", true, 4L, Instant.parse("2026-04-12T15:21:00Z")));

        mockMvc.perform(get("/api/v1/admin/sobrestamento/TEMA_9/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temaEncontrado").value(true))
                .andExpect(jsonPath("$.data.pendentes").value(4));
    }
}
