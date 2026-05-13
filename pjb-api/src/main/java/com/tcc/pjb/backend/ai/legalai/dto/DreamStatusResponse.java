package com.tcc.pjb.backend.ai.legalai.dto;

public record DreamStatusResponse(
        String dreamId,
        String status,
        String outputStoreId,
        Long inputTokens,
        Long outputTokens,
        String erro
) {}
