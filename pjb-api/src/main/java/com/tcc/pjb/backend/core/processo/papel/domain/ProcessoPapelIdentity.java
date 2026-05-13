package com.tcc.pjb.backend.core.processo.papel.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoPapelIdentity(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        String ritoProcessual,
        String faseProcessual,
        String statusProcessual,
        List<String> marcadores
) {
    public ProcessoPapelIdentity {
        Objects.requireNonNull(processoId);
        Objects.requireNonNull(numeroProcesso);
        ramoDireito = ramoDireito == null ? "NAO_INFORMADO" : ramoDireito;
        ritoProcessual = ritoProcessual == null ? "NAO_INFORMADO" : ritoProcessual;
        faseProcessual = faseProcessual == null ? "NAO_INFORMADO" : faseProcessual;
        statusProcessual = statusProcessual == null ? "NAO_INFORMADO" : statusProcessual;
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }
}
