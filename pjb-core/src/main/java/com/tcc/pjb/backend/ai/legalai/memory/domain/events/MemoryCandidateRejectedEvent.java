package com.tcc.pjb.backend.ai.legalai.memory.domain.events;

import java.time.Instant;
import java.util.UUID;

public record MemoryCandidateRejectedEvent(
        UUID candidateId,
        UUID reviewId,
        String revisadoPor,
        String motivo,
        Instant rejeitadoEm
) {}
