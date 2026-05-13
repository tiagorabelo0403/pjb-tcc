package com.tcc.pjb.backend.model.dto.audiencia;

public record AudienciaWebRtcOfferRequest(
        Long audienciaId,
        String sessaoToken,
        String sdpOffer
) {
}
