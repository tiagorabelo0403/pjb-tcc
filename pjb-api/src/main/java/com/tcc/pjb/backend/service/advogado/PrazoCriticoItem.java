package com.tcc.pjb.backend.service.advogado;

import java.time.Instant;

public record PrazoCriticoItem(
        Long workItemId,
        String titulo,
        Instant dueAt,
        long horasRestantes,
        String numeroProcesso
) {}
