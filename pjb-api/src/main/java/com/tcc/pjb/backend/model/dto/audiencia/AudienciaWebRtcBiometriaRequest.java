package com.tcc.pjb.backend.model.dto.audiencia;

public record AudienciaWebRtcBiometriaRequest(
        String sessaoToken,
        String referenciaHash,
        Double similaridade,
        String dispositivoId
) {
}
