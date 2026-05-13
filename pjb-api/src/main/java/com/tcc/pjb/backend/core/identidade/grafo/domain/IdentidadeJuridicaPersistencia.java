package com.tcc.pjb.backend.core.identidade.grafo.domain;

import java.time.Instant;
import java.util.Objects;

public record IdentidadeJuridicaPersistencia(
        String backend,
        int verticesPersistidos,
        int arestasPersistidas,
        Instant persistidoEm
) {
    public IdentidadeJuridicaPersistencia {
        backend = Objects.toString(backend, "MEMORY").trim();
        verticesPersistidos = Math.max(0, verticesPersistidos);
        arestasPersistidas = Math.max(0, arestasPersistidas);
        persistidoEm = persistidoEm == null ? Instant.now() : persistidoEm;
    }
}
