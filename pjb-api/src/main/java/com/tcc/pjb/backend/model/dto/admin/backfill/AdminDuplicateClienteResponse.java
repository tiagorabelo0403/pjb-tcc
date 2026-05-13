package com.tcc.pjb.backend.model.dto.admin.backfill;

public record AdminDuplicateClienteResponse(
        Long id,
        String nomeCompleto,
        Long advogadoId,
        Long equipeId,
        String status,
        String dataCriacao
) {}
