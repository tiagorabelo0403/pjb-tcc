package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.digitalizacao.DigitalizacaoApplicationService;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoConfiancaResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoEngineHealthResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoGovernanceBatchResult;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewQueueEntry;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoReviewQueueSnapshot;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminDigitalizacaoControllerTest {

    private DigitalizacaoApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(DigitalizacaoApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDigitalizacaoController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void reviewQueue_deveExporFilaDeRevisao() throws Exception {
        when(applicationService.reviewQueue(5)).thenReturn(new DigitalizacaoReviewQueueSnapshot(
                List.of(new DigitalizacaoReviewQueueEntry(1L, 7, Instant.parse("2026-04-11T12:00:00Z"))),
                1));

        mockMvc.perform(get("/api/v1/admin/digitalizacao/review-queue").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.jobs[0].jobId").value(1));
    }

    @Test
    void engineHealth_deveExporSaudeDoEngine() throws Exception {
        when(applicationService.engineHealth(org.mockito.ArgumentMatchers.any())).thenReturn(new DigitalizacaoEngineHealthResult(true, "ocr engine disponível", Instant.parse("2026-04-11T12:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/digitalizacao/engine/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.mensagem").value("ocr engine disponível"));
    }

    @Test
    void confianca_deveExporResultadoDoJob() throws Exception {
        when(applicationService.confianca(11L)).thenReturn(new DigitalizacaoConfiancaResult(11L, 66.5, true, 2));

        mockMvc.perform(get("/api/v1/admin/digitalizacao/jobs/11/confianca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value(11))
                .andExpect(jsonPath("$.data.confiancaMedia").value(66.5))
                .andExpect(jsonPath("$.data.paginasComRevisao").value(2));
    }

    @Test
    void reconcileStaleProcessing_deveExporResultadoDeComando() throws Exception {
        when(applicationService.reconcileStaleProcessing()).thenReturn(new DigitalizacaoGovernanceBatchResult(3));

        mockMvc.perform(post("/api/v1/admin/digitalizacao/governance/reconcile-stale-processing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("reconciliação de digitalização executada"))
                .andExpect(jsonPath("$.data.totalMarcadosComoFalha").value(3));
    }
}
