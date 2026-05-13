package com.tcc.pjb.backend.ai.legalai.memory.domain.events;

import java.time.Instant;
import java.util.UUID;

public record MemoryContradictionDetectedEvent(
        UUID candidateId,
        UUID storeId,
        String chaveConflitante,
        Instant detectadoEm
) {}
