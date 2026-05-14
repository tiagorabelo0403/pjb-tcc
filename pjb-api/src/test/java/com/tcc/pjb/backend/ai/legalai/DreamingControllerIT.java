package com.tcc.pjb.backend.ai.legalai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.legalai.dreaming.application.DreamingOrchestrator;
import com.tcc.pjb.backend.ai.legalai.dreaming.application.LegalAiDreamingMetricsService;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.Dream;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamId;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamInput;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamStatus;
import com.tcc.pjb.backend.ai.legalai.dto.DreamingSessionRequest;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DreamingControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DreamingOrchestrator orchestrator = mock(DreamingOrchestrator.class);
    private final LegalAiDreamingMetricsService metricsService = mock(LegalAiDreamingMetricsService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DreamingController(orchestrator, metricsService)).build();
    }

    @Test
    void deveAceitarSessaoDeReflexaoERetornarDreamIdComStatusPending() throws Exception {
        DreamId dreamId = DreamId.gerar();
        when(orchestrator.iniciarDreaming(any(), any(), any())).thenReturn(dreamId);

        DreamingSessionRequest request = new DreamingSessionRequest(
                UUID.randomUUID().toString(),
                List.of("session-1", "session-2"),
                "Consolidar aprendizados jurídicos desta sessão",
                "claude-sonnet-4-6"
        );

        mockMvc.perform(post("/api/v1/legal-ai/dreaming/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.dreamId").value(dreamId.value().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void deveRetornarStatusDoDreamPorId() throws Exception {
        MemoryStoreId storeId = MemoryStoreId.gerar();
        DreamId dreamId = DreamId.gerar();
        Dream dream = Dream.criar(dreamId, new DreamInput.MemoryStoreInput(storeId), "instrucoes", "claude-sonnet-4-6", Instant.now());
        Dream emExecucao = dream.comAnthropicId("anthropic_dream_123");

        when(orchestrator.consultar(any())).thenReturn(Optional.of(emExecucao));

        mockMvc.perform(get("/api/v1/legal-ai/dreaming/{dreamId}", dreamId.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dreamId").value(dreamId.value().toString()))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void deveRetornar404QuandoDreamNaoEncontrado() throws Exception {
        when(orchestrator.consultar(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/legal-ai/dreaming/{dreamId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveCancelarDreamERetornarStatusCancelado() throws Exception {
        MemoryStoreId storeId = MemoryStoreId.gerar();
        DreamId dreamId = DreamId.gerar();
        Dream dream = Dream.criar(dreamId, new DreamInput.MemoryStoreInput(storeId), null, "claude-sonnet-4-6", Instant.now());
        Dream cancelado = dream.cancelado(Instant.now());

        when(orchestrator.cancelar(any())).thenReturn(cancelado);

        mockMvc.perform(post("/api/v1/legal-ai/dreaming/{dreamId}/cancel", dreamId.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void deveRetornar400QuandoRequestInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/legal-ai/dreaming/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarDreamCompletadoComOutputStoreId() throws Exception {
        MemoryStoreId storeId = MemoryStoreId.gerar();
        MemoryStoreId outputStoreId = MemoryStoreId.gerar();
        DreamId dreamId = DreamId.gerar();
        Dream dream = Dream.criar(dreamId, new DreamInput.MemoryStoreInput(storeId), null, "claude-sonnet-4-6", Instant.now());
        Dream completado = dream.comAnthropicId("anthropic_123")
                .completado(new com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamResult(
                        outputStoreId, "anthrop_out_store", 1500L, 800L), Instant.now());

        when(orchestrator.consultar(any())).thenReturn(Optional.of(completado));

        mockMvc.perform(get("/api/v1/legal-ai/dreaming/{dreamId}", dreamId.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.outputStoreId").value(outputStoreId.value().toString()))
                .andExpect(jsonPath("$.inputTokens").value(1500))
                .andExpect(jsonPath("$.outputTokens").value(800));
    }
}
