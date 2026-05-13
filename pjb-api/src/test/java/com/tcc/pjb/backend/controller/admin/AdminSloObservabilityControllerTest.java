package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.PjbSloApplicationService;
import com.tcc.pjb.backend.core.observability.PjbSloRegistry;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminSloObservabilityControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PjbSloRegistry sloRegistry = new PjbSloRegistry(new SimpleMeterRegistry());
        ReflectionTestUtils.invokeMethod(sloRegistry, "registerSlos");
        PjbSloApplicationService applicationService = new PjbSloApplicationService(sloRegistry, mock(AuditLedgerService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminSloObservabilityController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void registry_deveExporSnapshotComOperacoesCriticas() throws Exception {
        mockMvc.perform(get("/api/v1/admin/observability/slo/registry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.operations.length()").value(9))
                .andExpect(jsonPath("$.data.operations[0].operation").value("peticionamento"));
    }

    @Test
    void evaluate_deveExporViolacaoDaOperacao() throws Exception {
        mockMvc.perform(get("/api/v1/admin/observability/slo/operations/peticionamento/evaluate")
                        .param("measuredSeconds", "4.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operation").value("peticionamento"))
                .andExpect(jsonPath("$.data.sloSeconds").value(3.0))
                .andExpect(jsonPath("$.data.violated").value(true));
    }

    @Test
    void consistency_deveExporSaudeDoRegistro() throws Exception {
        mockMvc.perform(get("/api/v1/admin/observability/slo/operations/mni_remessa/consistency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operation").value("mni_remessa"))
                .andExpect(jsonPath("$.data.consistent").value(true))
                .andExpect(jsonPath("$.data.source").value("pjb-slo-registry"));
    }
}
