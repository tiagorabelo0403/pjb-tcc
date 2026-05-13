package com.tcc.pjb.backend.core.processo.encaixe.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoEncaixeFinalAggregate(
        Long processoId,
        String numeroProcesso,
        String readiness,
        long score,
        long totalFindings,
        long totalBloqueantes,
        List<String> eixos,
        List<ProcessoEncaixeFinding> findings,
        List<String> acoesCorretivas,
        Instant geradoEm
) {
    public ProcessoEncaixeFinalAggregate {
        Objects.requireNonNull(processoId);
        numeroProcesso = numeroProcesso == null ? "NAO_INFORMADO" : numeroProcesso;
        readiness = readiness == null ? "NAO_AVALIADO" : readiness;
        eixos = eixos == null ? List.of() : List.copyOf(eixos);
        findings = findings == null ? List.of() : List.copyOf(findings);
        acoesCorretivas = acoesCorretivas == null ? List.of() : List.copyOf(acoesCorretivas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
