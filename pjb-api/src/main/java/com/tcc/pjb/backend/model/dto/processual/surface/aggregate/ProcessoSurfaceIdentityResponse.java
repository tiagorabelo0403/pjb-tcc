package com.tcc.pjb.backend.model.dto.processual.surface.aggregate;

import java.util.List;

public record ProcessoSurfaceIdentityResponse(
        Long processoId,
        String numeroProcesso,
        String tribunal,
        String uf,
        String comarca,
        String unidade,
        String ramo,
        String rito,
        String fase,
        String status,
        List<String> marcadores
) {
}
