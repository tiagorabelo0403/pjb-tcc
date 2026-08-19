package com.tcc.pjb.backend.model.dto.prazo;

import java.time.Instant;

public record PrazoCertidaoDecursoItemResponse(
        Long cienciaId,
        Long processoId,
        String numeroProcesso,
        String textoCertidao,
        Instant geradaEm
) {}
