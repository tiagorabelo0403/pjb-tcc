package com.tcc.pjb.backend.core.processo.documental.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoDocumentoAggregate(
        ProcessoDocumentoIdentity identity,
        long totalDocumentos,
        long lotes,
        long minutas,
        long assinados,
        long custodiados,
        long publicados,
        List<ProcessoDocumentoLote> grupos,
        List<String> alertas,
        List<String> trilhaAssinavel,
        Instant generatedAt
) {
    public ProcessoDocumentoAggregate {
        Objects.requireNonNull(identity);
        Objects.requireNonNull(generatedAt);
        grupos = grupos == null ? List.of() : List.copyOf(grupos);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        trilhaAssinavel = trilhaAssinavel == null ? List.of() : List.copyOf(trilhaAssinavel);
    }
}
