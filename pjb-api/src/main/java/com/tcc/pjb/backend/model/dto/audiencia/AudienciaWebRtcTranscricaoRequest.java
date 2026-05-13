package com.tcc.pjb.backend.model.dto.audiencia;

public record AudienciaWebRtcTranscricaoRequest(
        String sessaoToken,
        String trecho,
        Integer sequencia,
        boolean parcial
) {
}
