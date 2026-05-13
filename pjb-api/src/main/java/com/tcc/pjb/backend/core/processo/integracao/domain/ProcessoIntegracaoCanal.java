package com.tcc.pjb.backend.core.processo.integracao.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ProcessoIntegracaoCanal(
        String codigo,
        String titulo,
        String sistema,
        boolean habilitado,
        boolean operacional,
        String modoAutenticacao,
        boolean exigeCertificado,
        boolean permiteDryRun,
        boolean permiteSync,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {
    public ProcessoIntegracaoCanal {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        sistema = sistema == null ? "OUTRO" : sistema;
        modoAutenticacao = modoAutenticacao == null ? "NONE" : modoAutenticacao;
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }
}
