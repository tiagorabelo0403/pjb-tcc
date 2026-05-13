package com.tcc.pjb.backend.ai.legalai.dreaming.domain;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;

public record DreamResult(
        MemoryStoreId outputStoreId,
        String anthropicOutputStoreId,
        long inputTokens,
        long outputTokens
) {}
