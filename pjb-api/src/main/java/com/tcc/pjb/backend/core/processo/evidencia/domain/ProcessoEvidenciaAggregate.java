package com.tcc.pjb.backend.core.processo.evidencia.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ProcessoEvidenciaAggregate(
        Long processoIdRaiz,
        String numeroProcessoRaiz,
        UUID documentoIdRaiz,
        String sha256Raiz,
        boolean haCompartilhamentoInterfeitos,
        long processosCorrelatos,
        List<ProcessoEvidenciaItem> itens,
        List<String> alertas,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoEvidenciaAggregate {
        numeroProcessoRaiz = Objects.toString(numeroProcessoRaiz, "").trim();
        sha256Raiz = Objects.toString(sha256Raiz, "").trim().toLowerCase();
        itens = itens == null ? List.of() : List.copyOf(itens);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
