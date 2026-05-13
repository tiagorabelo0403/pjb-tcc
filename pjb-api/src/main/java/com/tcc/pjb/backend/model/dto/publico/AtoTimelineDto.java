package com.tcc.pjb.backend.model.dto.publico;

import java.time.Instant;

public record AtoTimelineDto(
        String descricaoSimples,
        Instant data,
        StatusAto status,
        String responsavel,
        String docUrl
) {
    public enum StatusAto {
        CONCLUIDO,
        PENDENTE,
        ATRASADO
    }
}
