package com.tcc.pjb.backend.model.dto.prazo;

import java.time.Instant;

public record PrazoCartorioItemResponse(
        Long cienciaId,
        Long processoId,
        String numeroProcesso,
        String tipoCiencia,
        Instant dataExpiracao,
        long diasRestantes,
        boolean vencido
) {}
