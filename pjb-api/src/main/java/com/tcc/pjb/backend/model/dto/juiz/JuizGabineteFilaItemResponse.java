package com.tcc.pjb.backend.model.dto.juiz;

import java.time.Instant;

public record JuizGabineteFilaItemResponse(
        Long workItemId,
        String titulo,
        String tipo,
        Instant dueAt,
        Integer prioridade,
        String status,
        Long processoId,
        String queueCode,
        String inboxKey,
        String deskAxis,
        boolean blocking
) {
}
