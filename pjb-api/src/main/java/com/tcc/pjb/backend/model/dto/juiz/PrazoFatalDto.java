package com.tcc.pjb.backend.model.dto.juiz;

import java.time.Instant;

public record PrazoFatalDto(
        Long workItemId,
        Long processoId,
        String titulo,
        Instant dueAt,
        long horasRestantes,
        boolean vencido
) {
}
