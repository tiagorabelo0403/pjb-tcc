package com.tcc.pjb.backend.model.dto.juiz;

import java.time.Instant;
import java.util.List;

public record GabineteAgrupadorItemDto(
        Long workItemId,
        Long processoId,
        String numeroProcesso,
        String titulo,
        String ramoDireito,
        String classeProcessual,
        String urgencyBand,
        Integer prioridade,
        Instant dueAt,
        boolean blocking,
        List<String> riskFlags
) {
}
