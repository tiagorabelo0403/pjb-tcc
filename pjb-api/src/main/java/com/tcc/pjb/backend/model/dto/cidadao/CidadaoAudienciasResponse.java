package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDate;
import java.util.List;

public record CidadaoAudienciasResponse(
        LocalDate from,
        LocalDate to,
        int total,
        List<DiaAudiencias> dias,
        AreaLinks links
) {
    public record DiaAudiencias(
            LocalDate dia,
            List<CidadaoAudienciaDto> audiencias
    ) {}
}
