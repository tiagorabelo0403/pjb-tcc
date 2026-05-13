package com.tcc.pjb.backend.ai.legalai.memory.domain.events;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateType;

import java.time.Instant;
import java.util.UUID;

public record LegalAiMemoryCandidateDetectedEvent(
        UUID candidateId,
        String sessionId,
        MemoryCandidateType tipo,
        boolean aprovacaoAutomatica,
        Instant detectadoEm
) {}
