package com.tcc.pjb.backend.core.processo.encaixe.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoEncaixeResumo(
        Long processoId,
        String numeroProcesso,
        String readiness,
        long score,
        long bloqueantes,
        long totalFindings,
        List<String> topFindings
) {
    public ProcessoEncaixeResumo {
        Objects.requireNonNull(processoId);
        numeroProcesso = numeroProcesso == null ? "NAO_INFORMADO" : numeroProcesso;
        readiness = readiness == null ? "NAO_AVALIADO" : readiness;
        topFindings = topFindings == null ? List.of() : List.copyOf(topFindings);
    }
}
