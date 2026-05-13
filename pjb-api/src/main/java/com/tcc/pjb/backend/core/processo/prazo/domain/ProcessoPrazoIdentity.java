package com.tcc.pjb.backend.core.processo.prazo.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoPrazoIdentity(
        Long processoId,
        String numeroProcesso,
        String tribunal,
        String uf,
        String comarca,
        String unidadeJudiciaria,
        String ramo,
        String rito,
        String fase,
        String status,
        List<String> marcadores
) {
    public ProcessoPrazoIdentity {
        Objects.requireNonNull(processoId);
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }
}
