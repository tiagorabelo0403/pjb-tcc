package com.tcc.pjb.backend.model.dto.desembargador;

public record RelatorPlenarioResultadoDto(
        int favor,
        int contra,
        int parcial,
        int outros,
        String tendencia,
        boolean unanimidade
) {
}
