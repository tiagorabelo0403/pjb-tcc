package com.tcc.pjb.backend.core.processo.prevencao.domain;

import com.tcc.pjb.backend.core.identidade.vinculo.domain.IdentidadeJuridicaVinculoAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoVinculacaoAnaliseAggregate(
        Long processoIdRaiz,
        String numeroProcessoRaiz,
        List<ProcessoVinculacaoAnaliseItem> itens,
        List<String> fundamentos,
        IdentidadeJuridicaVinculoAggregate vinculo,
        Instant geradoEm
) {
    public ProcessoVinculacaoAnaliseAggregate {
        numeroProcessoRaiz = Objects.toString(numeroProcessoRaiz, "").trim();
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        Objects.requireNonNull(vinculo, "vinculo");
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
