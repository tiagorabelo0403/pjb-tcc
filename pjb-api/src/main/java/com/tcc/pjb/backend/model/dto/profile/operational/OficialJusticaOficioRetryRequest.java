package com.tcc.pjb.backend.model.dto.profile.operational;

public record OficialJusticaOficioRetryRequest(
        String motivo,
        Integer prioridade,
        String novoCanalPreferencial
) {
}
