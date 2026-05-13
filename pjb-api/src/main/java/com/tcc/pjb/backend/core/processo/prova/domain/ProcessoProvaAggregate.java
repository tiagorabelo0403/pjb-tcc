package com.tcc.pjb.backend.core.processo.prova.domain;

import com.tcc.pjb.backend.core.processo.evidencia.domain.ProcessoEvidenciaAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoProvaAggregate(
        ProcessoProvaIdentity identity,
        ProcessoProvaIntegridade integridade,
        ProcessoProvaClassificacao classificacao,
        ProcessoEvidenciaAggregate evidencia,
        List<ProcessoProvaEvento> trilha,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoProvaAggregate {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(integridade, "integridade");
        Objects.requireNonNull(classificacao, "classificacao");
        Objects.requireNonNull(evidencia, "evidencia");
        trilha = trilha == null ? List.of() : List.copyOf(trilha);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
