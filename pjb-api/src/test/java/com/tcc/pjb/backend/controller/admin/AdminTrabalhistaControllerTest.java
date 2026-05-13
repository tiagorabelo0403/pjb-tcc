package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.financeiro.trabalhista.TrabalhistaApplicationService;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GruTrabalhistaResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaFluxoStatusResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaOwnershipView;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaTimelineAuditSnapshot;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminTrabalhistaControllerTest {

    private TrabalhistaApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(TrabalhistaApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminTrabalhistaController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void gerarGru_deveExporComandoAdministrativo() throws Exception {
        when(applicationService.gerarGru(11L, "PREPARO_RECURSAL", java.math.BigDecimal.valueOf(1500)))
                .thenReturn(new GruTrabalhistaResult(20L, "123", "456", LocalDate.of(2026, 5, 1)));

        mockMvc.perform(post("/api/v1/admin/trabalhista/processos/11/gru")
                        .param("tipo", "PREPARO_RECURSAL")
                        .param("valor", "1500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("gru trabalhista gerada"))
                .andExpect(jsonPath("$.data.gruId").value(20));
    }

    @Test
    void fluxoStatus_deveExporResumoDoProcesso() throws Exception {
        when(applicationService.fluxoStatus(9L)).thenReturn(new TrabalhistaFluxoStatusResult(9L, "EXECUCAO_TRABALHISTA", false, true, true));

        mockMvc.perform(get("/api/v1/admin/trabalhista/processos/9/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusProcesso").value("EXECUCAO_TRABALHISTA"))
                .andExpect(jsonPath("$.data.possuiGru").value(true));
    }

    @Test
    void timelineAudit_deveExporSnapshot() throws Exception {
        when(applicationService.timelineAudit(7L)).thenReturn(new TrabalhistaTimelineAuditSnapshot(7L, 2, true, false));

        mockMvc.perform(get("/api/v1/admin/trabalhista/processos/7/timeline/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEventos").value(2))
                .andExpect(jsonPath("$.data.possuiGru").value(true));
    }

    @Test
    void ownership_deveExporOwnershipDaTrilha() throws Exception {
        when(applicationService.ownership(4L)).thenReturn(new TrabalhistaOwnershipView("4", "EXECUCAO_TRABALHISTA", "TRT7"));

        mockMvc.perform(get("/api/v1/admin/trabalhista/processos/4/ownership"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referencia").value("4"))
                .andExpect(jsonPath("$.data.detalhe").value("TRT7"));
    }
}
