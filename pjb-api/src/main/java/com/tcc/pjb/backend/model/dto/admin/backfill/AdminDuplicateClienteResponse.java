package com.tcc.pjb.backend.model.dto.admin.backfill;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminDuplicateClienteResponse(
        Long id,
        String nomeCompleto,
        Long advogadoId,
        Long equipeId,
        String status,
        @Schema(description = "Data/hora de criação do registro", format = "date-time",
                example = "2026-06-01T10:00:00-03:00") String dataCriacao
) {}
