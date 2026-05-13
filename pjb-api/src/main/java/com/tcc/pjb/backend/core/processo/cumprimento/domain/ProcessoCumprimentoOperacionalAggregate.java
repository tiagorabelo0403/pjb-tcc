package com.tcc.pjb.backend.core.processo.cumprimento.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoCumprimentoOperacionalAggregate(Long processoId,
                                                      String numeroProcesso,
                                                      List<ProcessoCumprimentoOperacionalItem> itens,
                                                      int totalMaterializado,
                                                      int prioridadeMaxima,
                                                      boolean possuiBloqueio,
                                                      List<String> fundamentos,
                                                      Instant geradoEm) {
    public ProcessoCumprimentoOperacionalAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        itens = itens == null ? List.of() : List.copyOf(itens);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
