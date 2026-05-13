package com.tcc.pjb.backend.model.dto.advogado.surface;

import java.util.List;
import java.util.Map;

public record AdvogadoOperacaoResponse(
        String status,
        String tipo,
        Long processoId,
        Long workItemId,
        String dedupKey,
        Integer total,
        Integer processados,
        List<Long> ids,
        Map<String, Object> detalhes
) {}
