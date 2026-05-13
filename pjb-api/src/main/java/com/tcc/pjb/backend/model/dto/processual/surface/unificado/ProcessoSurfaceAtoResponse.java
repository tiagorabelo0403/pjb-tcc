package com.tcc.pjb.backend.model.dto.processual.surface.unificado;

import java.util.List;

public record ProcessoSurfaceAtoResponse(
        String codigo,
        String titulo,
        String categoria,
        boolean permitido,
        boolean sensivel,
        String motivo,
        List<String> alertas
) {
}
