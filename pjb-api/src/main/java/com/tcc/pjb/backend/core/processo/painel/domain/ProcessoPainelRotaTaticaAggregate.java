package com.tcc.pjb.backend.core.processo.painel.domain;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelRotaTaticaAggregate(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        List<ProcessoPainelRotaTaticaItem> itens,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoPainelRotaTaticaAggregate {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        ramoDireito = ramoDireito == null ? "NAO_INFORMADO" : ramoDireito;
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
