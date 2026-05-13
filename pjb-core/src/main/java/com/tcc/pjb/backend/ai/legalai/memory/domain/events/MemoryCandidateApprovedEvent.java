package com.tcc.pjb.backend.ai.legalai.memory.domain.events;

import java.time.Instant;
import java.util.UUID;

public record MemoryCandidateApprovedEvent(
        UUID candidateId,
        UUID reviewId,
        String revisadoPor,
        Instant aprovadoEm
) {}
