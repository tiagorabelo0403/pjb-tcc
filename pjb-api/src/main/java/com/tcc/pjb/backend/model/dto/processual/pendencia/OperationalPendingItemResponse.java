package com.tcc.pjb.backend.model.dto.processual.pendencia;

import java.time.Instant;

public record OperationalPendingItemResponse(
        Long workItemId,
        Long processoId,
        String numeroProcesso,
        String titulo,
        String type,
        String status,
        String fila,
        String inboxKey,
        Integer prioridade,
        Instant dueAt,
        boolean bloqueante,
        boolean atribuidoAoUsuario) {
}
