package com.tcc.pjb.backend.core.processo.migracao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoLegacyMirror(
        String codigo,
        String titulo,
        String sistema,
        String strategy,
        boolean active,
        Instant lastSyncAt,
        String status,
        List<String> divergencias
) {
    public ProcessoLegacyMirror {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        sistema = sistema == null ? "OUTRO" : sistema;
        strategy = strategy == null ? "SINGLE_WRITE" : strategy;
        status = status == null ? "NAO_AVALIADO" : status;
        divergencias = divergencias == null ? List.of() : List.copyOf(divergencias);
    }
}
