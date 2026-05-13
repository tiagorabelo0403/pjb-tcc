package com.tcc.pjb.backend.model.dto.processual.completude;

import java.util.List;

public record ProcessoCompletudeModuloResponse(
        String codigo,
        String status,
        int score,
        List<String> alertas
) {
    public ProcessoCompletudeModuloResponse {
        codigo = codigo == null ? "" : codigo;
        status = status == null ? "NAO_INFORMADO" : status;
        score = Math.max(0, Math.min(100, score));
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
    }
}
