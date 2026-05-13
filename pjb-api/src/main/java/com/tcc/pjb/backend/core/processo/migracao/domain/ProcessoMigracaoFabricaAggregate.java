package com.tcc.pjb.backend.core.processo.migracao.domain;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoMigracaoFabricaAggregate(
        Long processoId,
        String numeroProcesso,
        List<ProcessoMigracaoFabricaItem> itens,
        int scoreGeral,
        PjbFechamentoStatus statusGeral,
        boolean prontoMigracao,
        List<String> bloqueios,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoMigracaoFabricaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        itens = itens == null ? List.of() : List.copyOf(itens);
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        statusGeral = statusGeral == null ? PjbFechamentoStatus.PENDENTE : statusGeral;
        bloqueios = bloqueios == null ? List.of() : List.copyOf(bloqueios);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
