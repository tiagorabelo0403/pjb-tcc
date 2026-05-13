package com.tcc.pjb.backend.model.dto.audiencia;

import jakarta.validation.constraints.NotNull;

public record AudienciaWebRtcSessaoRequest(
        @NotNull Long audienciaId,
        Long processoId,
        String identificadorParticipante,
        boolean exigirBiometria
) {
}
