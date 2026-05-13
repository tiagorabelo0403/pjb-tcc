package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.peticionamento.saga.PeticionamentoSagaApplicationService;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.ProtocoloSagaPeticionamentoResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaHealthResult;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminPeticionamentoSagaControllerTest {

    private PeticionamentoSagaApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(PeticionamentoSagaApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPeticionamentoSagaController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void protocolo_deveExporNumero() throws Exception {
        when(applicationService.gerarProtocolo(8L)).thenReturn(new ProtocoloSagaPeticionamentoResult(8L, "P-2026-1", Instant.parse("2026-04-11T12:00:00Z"), "connector"));

        mockMvc.perform(post("/api/v1/admin/peticionamento/saga/8/protocolo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.numeroProtocolo").value("P-2026-1"));
    }

    @Test
    void health_deveExporStatus() throws Exception {
        when(applicationService.health(8L)).thenReturn(new SagaHealthResult(8L, "PROTOCOLO_GERADO", false));

        mockMvc.perform(get("/api/v1/admin/peticionamento/saga/8/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROTOCOLO_GERADO"));
    }
}
