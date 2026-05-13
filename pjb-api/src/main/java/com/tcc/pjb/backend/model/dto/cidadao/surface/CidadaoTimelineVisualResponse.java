package com.tcc.pjb.backend.model.dto.cidadao.surface;

import java.time.LocalDateTime;

public record CidadaoTimelineVisualResponse(
        Long processoId,
        String numero,
        String tribunal,
        String faseAtual,
        String descricaoSimples,
        LocalDateTime dataInicio,
        String proximoPasso
) {}
