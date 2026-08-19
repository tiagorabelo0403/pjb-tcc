package com.tcc.pjb.backend.model.dto.profile.operational;

import java.time.Instant;

public record PeritoHonorarioResumoDto(
        Long workItemId,
        Long processoId,
        String numeroProcesso,
        String titulo,
        String descricao,
        String status,
        Instant dueAt,
        Instant createdAt
) {}
