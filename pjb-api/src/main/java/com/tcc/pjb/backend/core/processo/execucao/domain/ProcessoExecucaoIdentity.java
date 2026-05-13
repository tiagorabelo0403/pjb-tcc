package com.tcc.pjb.backend.core.processo.execucao.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoExecucaoIdentity(
        Long processoId,
        String numeroProcesso,
        String tribunal,
        String ramoDireito,
        String ritoProcessual,
        String faseProcessual,
        String statusProcessual,
        List<String> marcadores
) {
    public ProcessoExecucaoIdentity {
        Objects.requireNonNull(processoId);
        Objects.requireNonNull(numeroProcesso);
        tribunal = tribunal == null ? "NAO_INFORMADO" : tribunal;
        ramoDireito = ramoDireito == null ? "NAO_INFORMADO" : ramoDireito;
        ritoProcessual = ritoProcessual == null ? "NAO_INFORMADO" : ritoProcessual;
        faseProcessual = faseProcessual == null ? "NAO_INFORMADO" : faseProcessual;
        statusProcessual = statusProcessual == null ? "NAO_INFORMADO" : statusProcessual;
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }
}
