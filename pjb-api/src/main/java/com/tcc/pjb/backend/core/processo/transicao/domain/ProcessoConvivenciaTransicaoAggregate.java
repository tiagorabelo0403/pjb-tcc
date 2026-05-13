package com.tcc.pjb.backend.core.processo.transicao.domain;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoConvivenciaTransicaoAggregate(
        Long processoId,
        String numeroProcesso,
        String tribunalPiloto,
        String ramoPiloto,
        List<ProcessoConvivenciaTransicaoTrack> tracks,
        int scoreGeral,
        PjbFechamentoStatus statusGeral,
        boolean prontoShadowMode,
        boolean prontoReversibilidade,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoConvivenciaTransicaoAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        tribunalPiloto = Objects.toString(tribunalPiloto, "").trim();
        ramoPiloto = Objects.toString(ramoPiloto, "").trim();
        tracks = tracks == null ? List.of() : List.copyOf(tracks);
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        statusGeral = statusGeral == null ? PjbFechamentoStatus.PENDENTE : statusGeral;
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
