package com.tcc.pjb.backend.core.processo.timeline.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoTimelineMalhaMaterializacaoAggregate(
        Long processoId,
        String numeroProcesso,
        int totalEventosPublicados,
        boolean publicouInboxOperacional,
        boolean publicouHistoricoProcessual,
        boolean atualizouSnapshotTemporal,
        List<String> canais,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoTimelineMalhaMaterializacaoAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        totalEventosPublicados = Math.max(0, totalEventosPublicados);
        canais = canais == null ? List.of() : List.copyOf(canais);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
