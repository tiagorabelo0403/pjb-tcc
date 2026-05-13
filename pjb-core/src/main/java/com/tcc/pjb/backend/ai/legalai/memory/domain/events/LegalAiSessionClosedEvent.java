package com.tcc.pjb.backend.ai.legalai.memory.domain.events;

import java.time.Instant;

public record LegalAiSessionClosedEvent(
        String sessionId,
        String processoId,
        String userId,
        String moduloOrigem,
        String sigiloNivel,
        int totalMessages,
        Instant encerradoEm
) {}
