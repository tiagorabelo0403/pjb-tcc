package com.tcc.pjb.backend.core.frontend.app.domain;

public record PjbFrontendProfessionalProcessSpotlightView(
        Long processoId,
        String numeroProcesso,
        String classeProcessual,
        String assunto,
        String tribunal,
        String uf,
        String comarca,
        String resultadoLabel,
        String accessBasis,
        String lastMovementLabel,
        String detailRoute,
        String cockpitRoute
) {
}
