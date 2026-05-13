package com.tcc.pjb.backend.core.processo.documental.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoDocumentoLote(
        String tituloBase,
        String eixoDocumental,
        String papelAssinante,
        boolean assinaturaObrigatoria,
        boolean bloqueadoPorAssinatura,
        boolean possuiVersaoCustodiada,
        int totalVersoes,
        String ultimaVersaoEstado,
        List<ProcessoDocumentoVersao> versoes,
        List<String> guardas
) {
    public ProcessoDocumentoLote {
        Objects.requireNonNull(tituloBase);
        Objects.requireNonNull(eixoDocumental);
        Objects.requireNonNull(papelAssinante);
        Objects.requireNonNull(ultimaVersaoEstado);
        versoes = versoes == null ? List.of() : List.copyOf(versoes);
        guardas = guardas == null ? List.of() : List.copyOf(guardas);
    }
}
