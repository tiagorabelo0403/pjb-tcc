package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.Map;

public record CalculoJudicialEconomicReferenceResponse(
        String referenciaTemporal,
        Map<String, Object> salarioMinimoNacional,
        Map<String, Object> inss,
        Map<String, Object> fontesOficiais,
        Map<String, Object> metadata,
        Instant geradoEm
) {
}
