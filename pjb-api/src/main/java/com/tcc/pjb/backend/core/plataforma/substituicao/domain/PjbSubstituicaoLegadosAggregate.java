package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbSubstituicaoLegadosAggregate(
        Long processoId,
        String numeroProcesso,
        List<PjbSubstituicaoLegadosProva> provas,
        List<PjbSubstituicaoLegadosSistema> sistemas,
        int scoreGeral,
        boolean prontoSubstituicaoImediata,
        String conclusaoTecnica,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoSubstituicaoResumo resumo() {
        return new ProcessoSubstituicaoResumo(scoreGeral, prontoSubstituicaoImediata, conclusaoTecnica, provas.stream().filter(PjbSubstituicaoLegadosProva::concluida).count(), provas.size());
    }

    public PjbSubstituicaoLegadosAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        provas = provas == null ? List.of() : List.copyOf(provas);
        sistemas = sistemas == null ? List.of() : List.copyOf(sistemas);
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        conclusaoTecnica = Objects.toString(conclusaoTecnica, "").trim();
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }

    public record ProcessoSubstituicaoResumo(int scoreGeral,
                                             boolean prontoSubstituicaoImediata,
                                             String conclusaoTecnica,
                                             long provasConcluidas,
                                             long totalProvas) {
    }
}
