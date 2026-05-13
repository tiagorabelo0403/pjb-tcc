package com.tcc.pjb.backend.core.processo.busca.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ProcessoBuscaAggregate(
        Map<String, String> filtros,
        int pagina,
        int tamanho,
        long totalAmostra,
        boolean filtragemPosPagina,
        List<ProcessoBuscaCard> resultados,
        List<ProcessoBuscaFacet> facets,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoBuscaAggregate {
        filtros = filtros == null ? Map.of() : Map.copyOf(filtros);
        resultados = resultados == null ? List.of() : List.copyOf(resultados);
        facets = facets == null ? List.of() : List.copyOf(facets);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
