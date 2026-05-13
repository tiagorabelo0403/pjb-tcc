package com.tcc.pjb.backend.core.processo.tese;

import java.time.Instant;
import java.util.Objects;

public record TeseVinculanteDecisao(
        String teseId,
        String tribunalOrigem,
        String ementa,
        String resultado,
        Instant julgadaEm,
        boolean aplicacaoImediata
) {
    public TeseVinculanteDecisao {
        Objects.requireNonNull(teseId, "teseId");
        Objects.requireNonNull(tribunalOrigem, "tribunalOrigem");
        Objects.requireNonNull(resultado, "resultado");
        julgadaEm = julgadaEm == null ? Instant.now() : julgadaEm;
    }
}
