package com.tcc.pjb.backend.core.processo.anomalia.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoAnomaliaGovernancaAggregate(
        Long processoId,
        String numeroProcesso,
        String nivelGlobal,
        int scoreGlobal,
        boolean exigiuPersistencia,
        Long securityAlertId,
        String canalGovernanca,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoAnomaliaGovernancaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        nivelGlobal = Objects.toString(nivelGlobal, "NORMAL").trim();
        scoreGlobal = Math.max(0, scoreGlobal);
        canalGovernanca = Objects.toString(canalGovernanca, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
