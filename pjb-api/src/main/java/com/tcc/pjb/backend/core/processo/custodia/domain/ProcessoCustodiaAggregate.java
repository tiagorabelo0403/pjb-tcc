package com.tcc.pjb.backend.core.processo.custodia.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ProcessoCustodiaAggregate(
        Long processoId,
        String numeroProcesso,
        UUID documentoId,
        ProcessoCustodiaAcao acao,
        NivelSigilo nivelSigiloEfetivo,
        boolean lacrada,
        boolean compartilhavel,
        String statusMalha,
        List<ProcessoCustodiaEvento> eventos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoCustodiaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        nivelSigiloEfetivo = nivelSigiloEfetivo == null ? NivelSigilo.PUBLICO : nivelSigiloEfetivo;
        statusMalha = Objects.toString(statusMalha, "").trim();
        eventos = eventos == null ? List.of() : List.copyOf(eventos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
