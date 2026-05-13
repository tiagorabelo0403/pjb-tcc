package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoPendenciasResponse(
        LocalDateTime geradoEm,
        int total,
        List<CidadaoPendenciaDto> pendencias,
        String legendUrl,
        AreaLinks links
) {}
