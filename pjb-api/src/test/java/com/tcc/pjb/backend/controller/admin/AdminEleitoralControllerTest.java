package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.eleitoral.EleitoralApplicationService;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralCalendarioHealthView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralConsultaFeitoResult;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralDiplomacaoSyncSummary;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralFeitoSnapshot;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralPendenciaDiplomacaoSnapshot;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralZonaHealthView;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminEleitoralControllerTest {

    private EleitoralApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(EleitoralApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminEleitoralController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void feito_deveExporConsultaDoFeito() throws Exception {
        when(applicationService.feito(9L)).thenReturn(new EleitoralConsultaFeitoResult(
                new EleitoralFeitoSnapshot(9L, "AIJE", "EM_ANDAMENTO", null),
                new EleitoralPendenciaDiplomacaoSnapshot(9L, "AIJE", "EM_ANDAMENTO")));

        mockMvc.perform(get("/api/v1/admin/eleitoral/processos/9/feito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.feito.processoId").value(9))
                .andExpect(jsonPath("$.data.feito.tipoFeito").value("AIJE"));
    }

    @Test
    void zonaHealth_deveExporSaudeDaZona() throws Exception {
        when(applicationService.zonaHealth(9L)).thenReturn(new EleitoralZonaHealthView("9", "OK", "cartorio=ZE083"));

        mockMvc.perform(get("/api/v1/admin/eleitoral/processos/9/zona/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referencia").value("9"))
                .andExpect(jsonPath("$.data.status").value("OK"))
                .andExpect(jsonPath("$.data.detalhe").value("cartorio=ZE083"));
    }

    @Test
    void diplomacaoSyncRun_deveExecutarRodadaManual() throws Exception {
        when(applicationService.diplomacaoSyncRun()).thenReturn(new EleitoralDiplomacaoSyncSummary(true, 4, Instant.parse("2026-04-11T12:00:00Z")));

        mockMvc.perform(post("/api/v1/admin/eleitoral/diplomacao/sync/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("sincronização de diplomação executada"))
                .andExpect(jsonPath("$.data.dryRun").value(true))
                .andExpect(jsonPath("$.data.pendentesAntes").value(4));
    }

    @Test
    void calendarioHealth_deveExporSaudeDaJanela() throws Exception {
        when(applicationService.calendarioHealth("CE", LocalDate.of(2026, 10, 1)))
                .thenReturn(new EleitoralCalendarioHealthView("CE", LocalDate.of(2026, 10, 1), true, "DIPLOMACAO"));

        mockMvc.perform(get("/api/v1/admin/eleitoral/calendario/health")
                        .param("uf", "CE")
                        .param("data", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uf").value("CE"))
                .andExpect(jsonPath("$.data.janelaAtiva").value(true))
                .andExpect(jsonPath("$.data.status").value("DIPLOMACAO"));
    }
}
