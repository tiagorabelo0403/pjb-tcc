package com.tcc.pjb.backend.core.processo.unificado.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoUnificadoIdentity(
        Long processoId,
        String numeroProcesso,
        String numeroUnificado,
        String tribunal,
        String uf,
        String comarca,
        String unidadeJudiciaria,
        String classeProcessual,
        String assunto,
        String parteAutora,
        String parteRe,
        List<String> etiquetas
) {
    public ProcessoUnificadoIdentity {
        Objects.requireNonNull(processoId);
        etiquetas = etiquetas == null ? List.of() : List.copyOf(etiquetas);
    }

    public List<String> marcadores() {
        return etiquetas;
    }
}

