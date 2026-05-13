package com.tcc.pjb.backend.ai.legalai.dto;

import java.time.Instant;

public record MemoryCandidateReviewResponse(
        String reviewId,
        String candidateId,
        String sessionId,
        String moduloOrigem,
        String tipo,
        String chave,
        String conteudo,
        String motivoExtracao,
        String sigiloNivel,
        String status,
        String revisadoPor,
        String motivoRejeicao,
        Instant criadoEm,
        Instant revisadoEm
) {}
