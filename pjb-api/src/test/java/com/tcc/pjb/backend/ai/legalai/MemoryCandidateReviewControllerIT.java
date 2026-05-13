package com.tcc.pjb.backend.ai.legalai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.legalai.dto.MemoryCandidateReviewDecisionRequest;
import com.tcc.pjb.backend.ai.legalai.memory.application.MemoryCandidateReviewService;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReview;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReviewId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReviewRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateType;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MemoryCandidateReviewControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MemoryCandidateReviewService reviewService = mock(MemoryCandidateReviewService.class);
    private final MemoryCandidateReviewRepository reviewRepository = mock(MemoryCandidateReviewRepository.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new MemoryCandidateReviewController(reviewService, reviewRepository)).build();
    }

    @Test
    void deveListarReviewsPendentes() throws Exception {
        MemoryCandidateReview review = reviewPendente();
        when(reviewRepository.listarPendentes()).thenReturn(List.of(review));

        mockMvc.perform(get("/api/v1/legal-ai/memory-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewId").value(review.id().value().toString()))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$[0].tipo").value("PROCESSUAL"));
    }

    @Test
    void deveBuscarReviewPorId() throws Exception {
        MemoryCandidateReview review = reviewPendente();
        when(reviewRepository.buscarPorId(any())).thenReturn(Optional.of(review));

        mockMvc.perform(get("/api/v1/legal-ai/memory-candidates/{reviewId}", review.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(review.id().value().toString()))
                .andExpect(jsonPath("$.moduloOrigem").value("sentencas"));
    }

    @Test
    void deveRetornar404QuandoReviewNaoEncontrada() throws Exception {
        when(reviewRepository.buscarPorId(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/legal-ai/memory-candidates/{reviewId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAprovarReviewERetornarStatusAprovado() throws Exception {
        MemoryCandidateReview review = reviewPendente();
        MemoryCandidateReview aprovada = review.aprovado("juiz.silva", Instant.now());
        when(reviewService.aprovar(any(), any())).thenReturn(aprovada);

        MemoryCandidateReviewDecisionRequest request = new MemoryCandidateReviewDecisionRequest("juiz.silva", null);

        mockMvc.perform(post("/api/v1/legal-ai/memory-candidates/{reviewId}/approve", review.id().value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.revisadoPor").value("juiz.silva"));
    }

    @Test
    void deveRejeitarReviewERetornarStatusRejeitado() throws Exception {
        MemoryCandidateReview review = reviewPendente();
        MemoryCandidateReview rejeitada = review.rejeitado("clerk.costa", "Dados imprecisos", Instant.now());
        when(reviewService.rejeitar(any(), any(), any())).thenReturn(rejeitada);

        MemoryCandidateReviewDecisionRequest request =
                new MemoryCandidateReviewDecisionRequest("clerk.costa", "Dados imprecisos");

        mockMvc.perform(post("/api/v1/legal-ai/memory-candidates/{reviewId}/reject", review.id().value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"))
                .andExpect(jsonPath("$.motivoRejeicao").value("Dados imprecisos"));
    }

    @Test
    void deveRetornar400QuandoRevisadoPorAusente() throws Exception {
        mockMvc.perform(post("/api/v1/legal-ai/memory-candidates/{reviewId}/approve", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoRejeicao\":\"teste\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar409QuandoReviewJaDecidida() throws Exception {
        when(reviewService.aprovar(any(), any()))
                .thenThrow(new IllegalStateException("Review já foi decidida: APROVADO"));

        MemoryCandidateReviewDecisionRequest request = new MemoryCandidateReviewDecisionRequest("juiz.silva", null);

        mockMvc.perform(post("/api/v1/legal-ai/memory-candidates/{reviewId}/approve", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    private MemoryCandidateReview reviewPendente() {
        return MemoryCandidateReview.criar(
                UUID.randomUUID(),
                "session-abc",
                "sentencas",
                MemoryCandidateType.PROCESSUAL,
                "PROCESSUAL:session-abc",
                "Sessão vinculada ao processo 1234567-89.2024.8.26.0001",
                "Contexto processual documentado",
                MemorySigiloNivel.INSTITUCIONAL,
                Instant.now()
        );
    }
}
