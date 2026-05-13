package com.tcc.pjb.backend.model.dto.juiz;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record GabineteAgrupadorResponse(
        Instant generatedAt,
        Long usuarioId,
        String usuarioNome,
        int totalItens,
        Map<String, List<GabineteAgrupadorItemDto>> porUrgencia,
        Map<String, List<GabineteAgrupadorItemDto>> porMateria,
        List<GabineteAgrupadorItemDto> comRiscoNulidade,
        List<GabineteAgrupadorItemDto> pendentesCriticos,
        Map<String, Long> contadores
) {
}
