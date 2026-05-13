package com.tcc.pjb.backend.core.processo.documental.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoDocumentoIdentity(
        Long processoId,
        String numeroProcesso,
        String ramo,
        String rito,
        String fase,
        String status,
        String tribunal,
        List<String> marcadores
) {
    public ProcessoDocumentoIdentity {
        Objects.requireNonNull(processoId);
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }
}
