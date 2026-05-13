package com.tcc.pjb.backend.model.dto.atendimento;

public record AtendimentoTosInfoResponse(
        int requiredVersion,
        String tosUrl,
        boolean accepted,
        int acceptedVersion
) {
}
