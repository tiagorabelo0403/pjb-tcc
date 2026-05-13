package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.platform.security.idempotency.PjbIdempotencyApplicationService;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyBudgetHealthView;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeyResult;
import com.tcc.pjb.backend.platform.security.idempotency.domain.PjbIdempotencyKeySnapshot;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminIdempotencyControllerTest {

    private PjbIdempotencyApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(PjbIdempotencyApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminIdempotencyController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void key_deveExporStatusDaChave() throws Exception {
        when(applicationService.key("idem-1")).thenReturn(new PjbIdempotencyKeyResult("idem-1", "PROCESSING", 5));

        mockMvc.perform(get("/api/v1/admin/idempotency/keys/idem-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").value("idem-1"))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    void budgetHealth_deveExporOrcamentoDaJanela() throws Exception {
        when(applicationService.budgetHealth()).thenReturn(new PjbIdempotencyBudgetHealthView(Duration.ofHours(24), 5, true, "janela configurada"));

        mockMvc.perform(get("/api/v1/admin/idempotency/budget/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(5))
                .andExpect(jsonPath("$.data.healthy").value(true));
    }

    @Test
    void release_deveExecutarComandoAdministrativo() throws Exception {
        when(applicationService.release("idem-2")).thenReturn(new PjbIdempotencyKeySnapshot("idem-2", null, Instant.parse("2026-04-11T10:00:00Z")));

        mockMvc.perform(post("/api/v1/admin/idempotency/keys/idem-2/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("chave de idempotencia liberada"))
                .andExpect(jsonPath("$.data.key").value("idem-2"));
    }
}
