package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record DiligenceRouteOptimizationRequest(
        Double origemLatitude,
        Double origemLongitude,
        Integer tempoMedioParadaMinutos,
        @Valid @NotEmpty List<StopInput> diligencias
) {

    public record StopInput(
            String id,
            String titulo,
            String endereco,
            Double latitude,
            Double longitude,
            Integer prioridade,
            Instant prazoFatalEm,
            Integer janelaInicioHora,
            Integer janelaFimHora
    ) {
    }
}
