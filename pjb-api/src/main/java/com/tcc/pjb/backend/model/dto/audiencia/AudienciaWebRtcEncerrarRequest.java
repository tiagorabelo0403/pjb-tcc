package com.tcc.pjb.backend.model.dto.audiencia;

public record AudienciaWebRtcEncerrarRequest(
        String sessaoToken,
        String gravacaoHash,
        String metricasResumo,
        boolean gravarTranscricaoFinal
) {
}
