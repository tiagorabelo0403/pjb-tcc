package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.time.Instant;
import java.util.UUID;

public record MemoryVersion(
        UUID id,
        UUID memoryEntryId,
        String conteudo,
        int versao,
        Instant criadoEm,
        boolean redacted
) {}
