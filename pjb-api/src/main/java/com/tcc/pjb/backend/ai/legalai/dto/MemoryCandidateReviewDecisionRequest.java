package com.tcc.pjb.backend.ai.legalai.dto;

import jakarta.validation.constraints.NotBlank;

public record MemoryCandidateReviewDecisionRequest(
        @NotBlank String revisadoPor,
        String motivoRejeicao
) {}
