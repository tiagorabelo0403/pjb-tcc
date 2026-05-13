package com.tcc.pjb.backend.core.events;

import java.time.Instant;
import java.util.Objects;

public record ProcessoSobrestatoEvent(
        Long processoId,
        String numeroUnificado,
        String teseVinculanteId,
        String tribunalQueDecidira,
        String fundamentoLegal,
        Instant sobrestatoEm
) {
    public ProcessoSobrestatoEvent {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(teseVinculanteId, "teseVinculanteId");
        sobrestatoEm = sobrestatoEm == null ? Instant.now() : sobrestatoEm;
    }
}
