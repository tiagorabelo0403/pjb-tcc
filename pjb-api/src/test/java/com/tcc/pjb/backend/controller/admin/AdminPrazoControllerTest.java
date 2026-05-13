package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.prazos.application.PrazoApplicationService;
import com.tcc.pjb.backend.core.prazos.auditoria.PrazoAuditTrail;
import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditHealthView;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoHealthResult;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminPrazoControllerTest {

    private PrazoApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(PrazoApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPrazoController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void health_deveExporDisponibilidade() throws Exception {
        when(applicationService.health("CE", "Fortaleza")).thenReturn(new PrazoHealthResult("CE", "Fortaleza", true, Instant.parse("2026-04-11T12:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/prazos/health").param("uf", "CE").param("comarca", "Fortaleza"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.calendarioDisponivel").value(true));
    }

    @Test
    void auditHealth_deveExporHash() throws Exception {
        when(applicationService.auditHealth(new com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditQuery(1L, "INTIMACAO", 2, PrazoRegime.UTEIS, LocalDateTime.parse("2026-04-11T09:00:00"), LocalDateTime.parse("2026-04-15T09:00:00"), "CE", "Fortaleza")))
                .thenReturn(new PrazoAuditHealthView("hash", 1));

        mockMvc.perform(get("/api/v1/admin/prazos/audit/health")
                        .param("processoId", "1")
                        .param("eventoRef", "INTIMACAO")
                        .param("quantidade", "2")
                        .param("regime", "UTEIS")
                        .param("inicio", "2026-04-11T09:00:00")
                        .param("fim", "2026-04-15T09:00:00")
                        .param("uf", "CE")
                        .param("comarca", "Fortaleza"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.calendarioVersaoHash").value("hash"));
    }
}
