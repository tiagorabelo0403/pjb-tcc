package com.tcc.pjb.backend.core.processo.trabalho.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoTrabalhoIdentity(
        Long processoId,
        String numeroProcesso,
        String ramo,
        String rito,
        String fase,
        String status,
        String tribunal,
        String unidade,
        List<String> marcadores
) {
    public ProcessoTrabalhoIdentity {
        Objects.requireNonNull(processoId);
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }
}
