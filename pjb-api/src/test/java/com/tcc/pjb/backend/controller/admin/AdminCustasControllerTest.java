package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.financeiro.custas.CustaJudicialService;
import com.tcc.pjb.backend.core.financeiro.custas.CustasApplicationService;
import com.tcc.pjb.backend.core.financeiro.custas.domain.CustaHealthResult;
import com.tcc.pjb.backend.core.financeiro.custas.domain.CustaStatusSnapshot;
import com.tcc.pjb.backend.core.financeiro.custas.domain.CustaVencimentoSnapshot;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminCustasControllerTest {

    private CustasApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(CustasApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCustasController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void gerar_deveExporResultado() throws Exception {
        when(applicationService.gerar(3L, "CUSTAS_INICIAIS", BigDecimal.TEN)).thenReturn(com.tcc.pjb.backend.core.financeiro.custas.domain.CustaJudicialResult.isento(9L, "gratuidade"));

        mockMvc.perform(post("/api/v1/admin/custas/gerar")
                        .param("processoId", "3")
                        .param("tipo", "CUSTAS_INICIAIS")
                        .param("valor", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.custaId").value(9))
                .andExpect(jsonPath("$.data.isento").value(true));
    }

    @Test
    void health_deveExporSnapshot() throws Exception {
        when(applicationService.health(9L)).thenReturn(new CustaHealthResult(
                new CustaStatusSnapshot(9L, "PENDENTE", Instant.parse("2026-04-11T12:00:00Z")),
                new CustaVencimentoSnapshot(9L, LocalDate.of(2026, 5, 1), false)
        ));

        mockMvc.perform(get("/api/v1/admin/custas/9/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.status").value("PENDENTE"))
                .andExpect(jsonPath("$.data.vencimento.vencida").value(false));
    }
}
