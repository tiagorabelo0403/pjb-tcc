package com.tcc.pjb.backend.core.processo.busca.domain;

import java.util.Objects;

public record ProcessoBuscaFacet(
        String eixo,
        String chave,
        long total
) {
    public ProcessoBuscaFacet {
        Objects.requireNonNull(eixo);
        Objects.requireNonNull(chave);
    }
}
